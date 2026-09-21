package tools.vitruv.framework.remote.modules.vsums.async;

import org.junit.jupiter.api.Test;
import tools.vitruv.change.interaction.ConfirmationUserInteraction;
import tools.vitruv.change.interaction.InteractionFactory;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Non-threaded state transitions and edge cases for {@link AsyncTaskStatus}.
 * Concurrency / wait-notify behaviour is covered separately in
 * {@link AsyncTaskStatusInteractionTest}.
 */
class AsyncTaskStatusStateTest {

    private AsyncTaskStatus newStatus() {
        return new AsyncTaskStatus(UUID.randomUUID(), UUID.randomUUID());
    }

    private static ConfirmationUserInteraction pending() {
        ConfirmationUserInteraction interaction = InteractionFactory.eINSTANCE.createConfirmationUserInteraction();
        interaction.setMessage("Proceed?");
        return interaction;
    }

    @Test
    void startsRunningAndNotCompleted() {
        AsyncTaskStatus status = newStatus();
        assertThat(status.getState()).isEqualTo(AsyncTaskState.RUNNING);
        assertThat(status.isCompleted()).isFalse();
        assertThat(status.getCreatedAt()).isNotNull();
        assertThat(status.isParkedAsInconsistency()).isFalse();
    }

    @Test
    void completeTransitionsToCompleted() {
        AsyncTaskStatus status = newStatus();
        status.complete("payload");
        assertThat(status.getState()).isEqualTo(AsyncTaskState.COMPLETED);
        assertThat(status.getResult()).isEqualTo("payload");
        assertThat(status.getCompletedAt()).isNotNull();
        assertThat(status.isCompleted()).isTrue();
    }

    @Test
    void failTransitionsToFailed() {
        AsyncTaskStatus status = newStatus();
        status.fail("kaboom");
        assertThat(status.getState()).isEqualTo(AsyncTaskState.FAILED);
        assertThat(status.getErrorMessage()).isEqualTo("kaboom");
        assertThat(status.getCompletedAt()).isNotNull();
        assertThat(status.isCompleted()).isTrue();
    }

    @Test
    void setWaitingForInteractionMovesToWaitingState() {
        AsyncTaskStatus status = newStatus();
        status.setWaitingForInteraction(pending());
        assertThat(status.getState()).isEqualTo(AsyncTaskState.WAITING_USER_INTERACTION);
        assertThat(status.isWaitingForUserInteraction()).isTrue();
        assertThat(status.getPendingInteraction()).isNotNull();
    }

    @Test
    void parkAsInconsistencyIsNoOpWhenNotWaiting() {
        AsyncTaskStatus status = newStatus();
        status.parkAsInconsistency();
        // Still RUNNING because there was no pending interaction to park.
        assertThat(status.getState()).isEqualTo(AsyncTaskState.RUNNING);
        assertThat(status.isParkedAsInconsistency()).isFalse();
    }

    @Test
    void validationErrorCanBeSetAndCleared() {
        AsyncTaskStatus status = newStatus();
        status.setValidationError("bad input");
        assertThat(status.getValidationError()).isEqualTo("bad input");
        status.clearValidationError();
        assertThat(status.getValidationError()).isNull();
    }

    @Test
    void committedResourceSetIsStored() {
        AsyncTaskStatus status = newStatus();
        status.setCommittedResourceSet("<xmi/>");
        assertThat(status.getCommittedResourceSet()).isEqualTo("<xmi/>");
    }

    @Test
    void completeClearsValidationError() {
        AsyncTaskStatus status = newStatus();
        status.setValidationError("bad input");
        status.complete("ok");
        assertThat(status.getValidationError()).isNull();
    }
}
