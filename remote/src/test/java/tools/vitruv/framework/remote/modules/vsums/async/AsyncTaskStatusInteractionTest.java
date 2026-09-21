package tools.vitruv.framework.remote.modules.vsums.async;

import org.junit.jupiter.api.Test;
import tools.vitruv.change.interaction.ConfirmationUserInteraction;
import tools.vitruv.change.interaction.InteractionFactory;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class AsyncTaskStatusInteractionTest {

    @Test
    void waitForInteractionResponseUnblocksAfterSetInteractionResponse() throws Exception {
        AsyncTaskStatus status = new AsyncTaskStatus(UUID.randomUUID(), UUID.randomUUID());

        ConfirmationUserInteraction pending = InteractionFactory.eINSTANCE.createConfirmationUserInteraction();
        pending.setMessage("Proceed?");
        status.setWaitingForInteraction(pending);

        ConfirmationUserInteraction[] workerResult = new ConfirmationUserInteraction[1];
        Thread worker = new Thread(() -> {
            try {
                workerResult[0] = status.waitForInteractionResponse();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        });
        worker.start();

        for (int attempt = 0; attempt < 20 && status.getState() != AsyncTaskState.WAITING_USER_INTERACTION; attempt++) {
            TimeUnit.MILLISECONDS.sleep(10);
        }
        assertThat(status.getState()).isEqualTo(AsyncTaskState.WAITING_USER_INTERACTION);

        ConfirmationUserInteraction response = InteractionFactory.eINSTANCE.createConfirmationUserInteraction();
        response.setMessage("Proceed?");
        response.setConfirmed(true);
        status.setInteractionResponse(response);

        worker.join(2_000);
        assertThat(worker.isAlive()).isFalse();
        assertThat(workerResult[0].isConfirmed()).isTrue();
        assertThat(status.getState()).isEqualTo(AsyncTaskState.RUNNING);
        assertThat(status.getPendingInteraction()).isNull();
    }

    @Test
    void parkAsInconsistencyKeepsWorkerWaiting() throws Exception {
        AsyncTaskStatus status = new AsyncTaskStatus(UUID.randomUUID(), UUID.randomUUID());

        ConfirmationUserInteraction pending = InteractionFactory.eINSTANCE.createConfirmationUserInteraction();
        pending.setMessage("Proceed?");
        status.setWaitingForInteraction(pending);

        ConfirmationUserInteraction[] workerResult = new ConfirmationUserInteraction[1];
        Thread worker = new Thread(() -> {
            try {
                workerResult[0] = status.waitForInteractionResponse();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        });
        worker.start();

        for (int attempt = 0; attempt < 20 && status.getState() != AsyncTaskState.WAITING_USER_INTERACTION; attempt++) {
            TimeUnit.MILLISECONDS.sleep(10);
        }
        assertThat(status.getState()).isEqualTo(AsyncTaskState.WAITING_USER_INTERACTION);

        status.parkAsInconsistency();
        TimeUnit.MILLISECONDS.sleep(100);
        assertThat(worker.isAlive()).isTrue();
        assertThat(status.isParkedAsInconsistency()).isTrue();
        assertThat(status.getState()).isEqualTo(AsyncTaskState.WAITING_USER_INTERACTION);

        ConfirmationUserInteraction response = InteractionFactory.eINSTANCE.createConfirmationUserInteraction();
        response.setMessage("Proceed?");
        response.setConfirmed(true);
        status.setInteractionResponse(response);

        worker.join(2_000);
        assertThat(worker.isAlive()).isFalse();
        assertThat(workerResult[0].isConfirmed()).isTrue();
        assertThat(status.getState()).isEqualTo(AsyncTaskState.RUNNING);
    }

    @Test
    void markInconsistentUnblocksWaitingWorker() throws Exception {
        AsyncTaskStatus status = new AsyncTaskStatus(UUID.randomUUID(), UUID.randomUUID());

        ConfirmationUserInteraction pending = InteractionFactory.eINSTANCE.createConfirmationUserInteraction();
        pending.setMessage("Proceed?");
        status.setWaitingForInteraction(pending);

        Throwable[] workerError = new Throwable[1];
        Thread worker = new Thread(() -> {
            try {
                status.waitForInteractionResponse();
            } catch (PropagationInconsistentException e) {
                workerError[0] = e;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        });
        worker.start();

        for (int attempt = 0; attempt < 20 && status.getState() != AsyncTaskState.WAITING_USER_INTERACTION; attempt++) {
            TimeUnit.MILLISECONDS.sleep(10);
        }
        assertThat(status.getState()).isEqualTo(AsyncTaskState.WAITING_USER_INTERACTION);

        status.markInconsistent();
        worker.join(2_000);

        assertThat(worker.isAlive()).isFalse();
        assertThat(workerError[0]).isInstanceOf(PropagationInconsistentException.class);
        assertThat(status.getState()).isEqualTo(AsyncTaskState.INCONSISTENT);
        assertThat(status.getPendingInteraction()).isNotNull();
        assertThat(status.isCompleted()).isTrue();
    }
}
