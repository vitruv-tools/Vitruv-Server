package tools.vitruv.framework.remote.modules.vsums.async;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.vitruv.change.interaction.ConfirmationUserInteraction;
import tools.vitruv.change.interaction.InteractionFactory;
import tools.vitruv.framework.remote.modules.vsums.async.AsyncTaskState;
import tools.vitruv.framework.remote.modules.vsums.async.AsyncTaskStatus;
import tools.vitruv.framework.remote.modules.vsums.async.PropagationTaskRegistry;
import tools.vitruv.framework.remote.modules.vsums.async.ServerInteractionResultProvider;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Pure unit tests for {@link PropagationTaskRegistry}: lifecycle bookkeeping,
 * one-active-task detection and active-task resolution (thread context vs. sole active).
 */
class PropagationTaskRegistryTest {

    private PropagationTaskRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new PropagationTaskRegistry();
        // Avoid contamination from any thread-local set by other code on this thread.
        ServerInteractionResultProvider.clearCurrentTaskId();
    }

    @AfterEach
    void tearDown() {
        ServerInteractionResultProvider.clearCurrentTaskId();
    }

    private static ConfirmationUserInteraction pendingInteraction() {
        ConfirmationUserInteraction interaction = InteractionFactory.eINSTANCE.createConfirmationUserInteraction();
        interaction.setMessage("Proceed?");
        return interaction;
    }

    @Test
    void registerCreatesRunningTaskThatCanBeLookedUp() {
        UUID taskId = UUID.randomUUID();
        UUID viewId = UUID.randomUUID();

        AsyncTaskStatus status = registry.register(taskId, viewId);

        assertThat(status.getState()).isEqualTo(AsyncTaskState.RUNNING);
        assertThat(status.getTaskId()).isEqualTo(taskId);
        assertThat(status.getViewId()).isEqualTo(viewId);
        assertThat(registry.getTaskStatus(taskId)).isSameAs(status);
        assertThat(registry.taskExists(taskId)).isTrue();
    }

    @Test
    void completeAndFailUpdateTerminalState() {
        UUID completed = UUID.randomUUID();
        UUID failed = UUID.randomUUID();
        registry.register(completed, UUID.randomUUID());
        registry.register(failed, UUID.randomUUID());

        registry.completeTask(completed, "done");
        registry.failTask(failed, "boom");

        assertThat(registry.getTaskStatus(completed).getState()).isEqualTo(AsyncTaskState.COMPLETED);
        assertThat(registry.getTaskStatus(completed).getResult()).isEqualTo("done");
        assertThat(registry.getTaskStatus(failed).getState()).isEqualTo(AsyncTaskState.FAILED);
        assertThat(registry.getTaskStatus(failed).getErrorMessage()).isEqualTo("boom");
    }

    @Test
    void lifecycleMethodsAreNullSafeForUnknownTasks() {
        UUID unknown = UUID.randomUUID();
        assertThatCode(() -> {
            registry.completeTask(unknown, "x");
            registry.failTask(unknown, "x");
            registry.parkAsInconsistency(unknown);
            registry.markInconsistent(unknown);
            registry.removeTask(unknown);
        }).doesNotThrowAnyException();
        assertThat(registry.getTaskStatus(unknown)).isNull();
        assertThat(registry.taskExists(unknown)).isFalse();
    }

    @Test
    void parkAsInconsistencyKeepsWaitingStateWhileMarkInconsistentIsTerminal() {
        UUID parked = UUID.randomUUID();
        AsyncTaskStatus parkedStatus = registry.register(parked, UUID.randomUUID());
        parkedStatus.setWaitingForInteraction(pendingInteraction());

        registry.parkAsInconsistency(parked);
        assertThat(parkedStatus.getState()).isEqualTo(AsyncTaskState.WAITING_USER_INTERACTION);
        assertThat(parkedStatus.isParkedAsInconsistency()).isTrue();

        UUID aborted = UUID.randomUUID();
        AsyncTaskStatus abortedStatus = registry.register(aborted, UUID.randomUUID());
        abortedStatus.setWaitingForInteraction(pendingInteraction());
        registry.markInconsistent(aborted);
        assertThat(abortedStatus.getState()).isEqualTo(AsyncTaskState.INCONSISTENT);
    }

    @Test
    void removeTaskDeletesTheEntry() {
        UUID taskId = UUID.randomUUID();
        registry.register(taskId, UUID.randomUUID());

        registry.removeTask(taskId);

        assertThat(registry.taskExists(taskId)).isFalse();
        assertThat(registry.getTaskStatus(taskId)).isNull();
    }

    @Test
    void hasActivePropagationTaskReflectsRunningAndWaitingTasksOnly() {
        assertThat(registry.hasActivePropagationTask()).isFalse();

        UUID running = UUID.randomUUID();
        UUID waiting = UUID.randomUUID();
        registry.register(running, UUID.randomUUID());
        AsyncTaskStatus waitingStatus = registry.register(waiting, UUID.randomUUID());
        waitingStatus.setWaitingForInteraction(pendingInteraction());
        assertThat(registry.hasActivePropagationTask()).isTrue();

        registry.completeTask(running, "ok");
        assertThat(registry.hasActivePropagationTask()).isTrue(); // waiting still active

        registry.markInconsistent(waiting);
        assertThat(registry.hasActivePropagationTask()).isFalse();
    }

    @Test
    void resolveActiveTaskIdPrefersThreadContext() {
        UUID threadTask = UUID.randomUUID();
        ServerInteractionResultProvider.setCurrentTaskId(threadTask);

        // Even with a different sole active task, the thread context wins.
        registry.register(UUID.randomUUID(), UUID.randomUUID());

        assertThat(registry.resolveActiveTaskId()).contains(threadTask);
    }

    @Test
    void resolveActiveTaskIdReturnsSoleActiveTaskWhenNoThreadContext() {
        UUID only = UUID.randomUUID();
        registry.register(only, UUID.randomUUID());

        assertThat(registry.resolveActiveTaskId()).contains(only);
    }

    @Test
    void resolveActiveTaskIdIsEmptyWhenMultipleActiveTasks() {
        registry.register(UUID.randomUUID(), UUID.randomUUID());
        registry.register(UUID.randomUUID(), UUID.randomUUID());

        assertThat(registry.resolveActiveTaskId()).isEqualTo(Optional.empty());
    }

    @Test
    void resolveActiveTaskIdIsEmptyWhenNoActiveTasks() {
        UUID taskId = UUID.randomUUID();
        registry.register(taskId, UUID.randomUUID());
        registry.completeTask(taskId, "done");

        assertThat(registry.resolveActiveTaskId()).isEqualTo(Optional.empty());
    }
}
