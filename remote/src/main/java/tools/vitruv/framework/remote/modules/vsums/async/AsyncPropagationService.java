package tools.vitruv.framework.remote.modules.vsums.async;

import tools.vitruv.framework.remote.modules.vsums.model.manager.ViewManager;
import tools.vitruv.framework.remote.modules.vsums.model.manager.VsumManager;
import tools.vitruv.framework.remote.modules.vsums.model.services.ViewService;
import tools.vitruv.framework.remote.modules.vsums.model.wrapper.ViewWrapper;
import tools.vitruv.framework.remote.modules.vsums.usecases.InconsistencyUseCases;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;
import java.util.concurrent.Executor;

@Service
public class AsyncPropagationService {

    private final PropagationTaskRegistry taskRegistry;
    private final ViewManager viewManager;
    private final ViewService viewService;
    private final InconsistencyUseCases inconsistencyUseCases;
    private final VsumManager vsumManager;
    private final Executor propagationExecutor;

    public AsyncPropagationService(
            PropagationTaskRegistry taskRegistry,
            ViewManager viewManager,
            ViewService viewService,
            InconsistencyUseCases inconsistencyUseCases,
            VsumManager vsumManager,
            @Qualifier("propagationExecutor") Executor propagationExecutor) {
        this.taskRegistry = taskRegistry;
        this.viewManager = viewManager;
        this.viewService = viewService;
        this.inconsistencyUseCases = inconsistencyUseCases;
        this.vsumManager = vsumManager;
        this.propagationExecutor = propagationExecutor;
    }

    public UUID startUpdate(UUID viewId) {
        return startUpdate(viewId, null);
    }

    /**
     * Starts async propagation. When {@code encodedResourceSet} is provided, commits it on the worker
     * thread before {@code view.update()} so user interactions during commit-time reactions can pause
     * via the task registry.
     */
    public UUID startUpdate(UUID viewId, String encodedResourceSet) {
        ViewWrapper viewWrapper = viewManager.getView(viewId);
        if (viewWrapper == null) {
            throw new IllegalArgumentException("View not found: " + viewId);
        }

        UUID vsumId = viewWrapper.vsumWrapper().info().getId();
        if (inconsistencyUseCases.hasOpenForVsum(vsumId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "VSUM has an open inconsistency; resolve it in the hub before applying new updates");
        }

        UUID taskId = UUID.randomUUID();
        taskRegistry.register(taskId, viewId);

        propagationExecutor.execute(() -> runPropagation(taskId, viewWrapper, encodedResourceSet));
        return taskId;
    }

    private void runPropagation(UUID taskId, ViewWrapper viewWrapper, String encodedResourceSet) {
        try {
            ServerInteractionResultProvider.setCurrentTaskId(taskId);
            if (encodedResourceSet != null && !encodedResourceSet.isBlank()) {
                viewService.commitResourceSet(viewWrapper, encodedResourceSet);
                AsyncTaskStatus status = taskRegistry.getTaskStatus(taskId);
                if (status != null) {
                    status.setCommittedResourceSet(encodedResourceSet);
                }
                viewService.saveViewSnapshot(viewWrapper);
            }
            String updatedResourceSet = viewService.update(viewWrapper);
            taskRegistry.completeTask(taskId, updatedResourceSet);
            inconsistencyUseCases.markResolvedByTaskId(taskId);
            // Parked hub flows leave the view open; close it after success so reopen works cleanly.
            if (taskRegistry.getTaskStatus(taskId) != null
                    && taskRegistry.getTaskStatus(taskId).isParkedAsInconsistency()) {
                try {
                    viewManager.removeAndCloseView(viewWrapper.viewId());
                } catch (RuntimeException closeError) {
                    // Propagation already succeeded; closing is best-effort.
                }
            }
        } catch (PropagationInconsistentException e) {
            taskRegistry.markInconsistent(taskId);
            // Dismiss/abort can leave the EMF change recorder mid-recording; reload next open.
            vsumManager.evictVsum(viewWrapper.vsumWrapper().info().getId());
        } catch (Exception e) {
            AsyncTaskStatus status = taskRegistry.getTaskStatus(taskId);
            if (status != null && status.getState() == AsyncTaskState.INCONSISTENT) {
                return;
            }
            String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            taskRegistry.failTask(taskId, message);
            inconsistencyUseCases.markFailedByTaskId(taskId, message);
            // "This recorder is already recording" and other mid-commit failures stick the VSUM in memory.
            if (message.contains("already recording") || message.contains("Changes rejected")) {
                vsumManager.evictVsum(viewWrapper.vsumWrapper().info().getId());
            }
        } finally {
            ServerInteractionResultProvider.clearCurrentTaskId();
        }
    }
}
