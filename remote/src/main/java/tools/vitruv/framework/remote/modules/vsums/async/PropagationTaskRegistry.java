package tools.vitruv.framework.remote.modules.vsums.async;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PropagationTaskRegistry {
  private final Map<UUID, AsyncTaskStatus> tasks = new ConcurrentHashMap<>();

  public AsyncTaskStatus register(UUID taskId, UUID viewId) {
    AsyncTaskStatus status = new AsyncTaskStatus(taskId, viewId);
    tasks.put(taskId, status);
    return status;
  }

  public AsyncTaskStatus getTaskStatus(UUID taskId) {
    return tasks.get(taskId);
  }

  public void completeTask(UUID taskId, String result) {
    AsyncTaskStatus status = tasks.get(taskId);
    if (status != null) {
      status.complete(result);
    }
  }

  public void failTask(UUID taskId, String errorMessage) {
    AsyncTaskStatus status = tasks.get(taskId);
    if (status != null) {
      status.fail(errorMessage);
    }
  }

  public void parkAsInconsistency(UUID taskId) {
    AsyncTaskStatus status = tasks.get(taskId);
    if (status != null) {
      status.parkAsInconsistency();
    }
  }

  /**
   * @deprecated abort-style park; prefer {@link #parkAsInconsistency(UUID)}
   */
  @Deprecated
  public void markInconsistent(UUID taskId) {
    AsyncTaskStatus status = tasks.get(taskId);
    if (status != null) {
      status.markInconsistent();
    }
  }

  public void removeTask(UUID taskId) {
    tasks.remove(taskId);
  }

  public boolean taskExists(UUID taskId) {
    return tasks.containsKey(taskId);
  }

  public boolean hasActivePropagationTask() {
    return tasks.values().stream().anyMatch(this::isActive);
  }

  /**
   * Resolves the task id for an in-flight async propagation.
   * Uses the worker thread context when available, otherwise the sole active task.
   */
  public Optional<UUID> resolveActiveTaskId() {
    Optional<UUID> taskIdFromThread = ServerInteractionResultProvider.getCurrentTaskId();
    if (taskIdFromThread.isPresent()) {
      return taskIdFromThread;
    }

    List<AsyncTaskStatus> activeTasks = tasks.values().stream().filter(this::isActive).toList();
    if (activeTasks.size() == 1) {
      return Optional.of(activeTasks.get(0).getTaskId());
    }
    return Optional.empty();
  }

  private boolean isActive(AsyncTaskStatus status) {
    return status.getState() == AsyncTaskState.RUNNING
        || status.getState() == AsyncTaskState.WAITING_USER_INTERACTION;
  }
}
