package tools.vitruv.framework.remote.server.async;

import java.util.ArrayList;
import java.util.List;

import tools.vitruv.change.interaction.ConfirmationUserInteraction;
import tools.vitruv.change.interaction.FreeTextUserInteraction;
import tools.vitruv.change.interaction.InteractionFactory;
import tools.vitruv.change.interaction.InteractionResultProvider;
import tools.vitruv.change.interaction.MultipleChoiceMultiSelectionUserInteraction;
import tools.vitruv.change.interaction.MultipleChoiceSingleSelectionUserInteraction;
import tools.vitruv.change.interaction.NotificationUserInteraction;
import tools.vitruv.change.interaction.UserInteractionOptions.InputValidator;
import tools.vitruv.change.interaction.UserInteractionOptions.NotificationType;
import tools.vitruv.change.interaction.UserInteractionOptions.WindowModality;
import tools.vitruv.framework.remote.server.registry.AsyncTaskRegistry;
import tools.vitruv.framework.remote.server.registry.AsyncTaskStatus;

/**
 * Server-side InteractionResultProvider that handles user interactions during
 * async change propagation.
 * Uses ThreadLocal to detect whether we're in an async context, then stores
 * interactions
 * in AsyncTaskStatus and blocks until the client responds via REST API.
 */
public class ServerInteractionResultProvider implements InteractionResultProvider {

    // ThreadLocal to store the current taskId for async propagation threads
    private static final ThreadLocal<String> CURRENT_TASK_ID = new ThreadLocal<>();

    /**
     * Sets the taskId for the current thread. Must be called before async
     * propagation starts.
     */
    public static void setCurrentTaskId(String taskId) {
        CURRENT_TASK_ID.set(taskId);
    }

    /**
     * Clears the taskId for the current thread. Should be called after async
     * propagation completes.
     */
    public static void clearCurrentTaskId() {
        CURRENT_TASK_ID.remove();
    }

    private AsyncTaskStatus getTaskStatus() {
        String taskId = CURRENT_TASK_ID.get();
        if (taskId == null) {
            throw new UnsupportedOperationException(
                    "User interactions are not supported in synchronous change propagation mode. " +
                            "Use async propagation endpoint instead.");
        }

        AsyncTaskStatus taskStatus = AsyncTaskRegistry.getInstance().getTaskStatus(taskId);
        if (taskStatus == null) {
            throw new IllegalStateException("Task not found in registry: " + taskId);
        }
        return taskStatus;
    }

    @Override
    public boolean getConfirmationInteractionResult(WindowModality windowModality, String title, String message,
            String positiveDecisionText, String negativeDecisionText, String cancelDecisionText) {
        AsyncTaskStatus taskStatus = getTaskStatus();

        // Create the interaction object
        ConfirmationUserInteraction interaction = InteractionFactory.eINSTANCE.createConfirmationUserInteraction();
        interaction.setMessage(message);

        // Store it in the task status and wait for response
        taskStatus.setWaitingForInteraction(interaction);

        // Block until client responds
        try {
            ConfirmationUserInteraction response = taskStatus.waitForInteractionResponse();
            return response.isConfirmed();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for confirmation response", e);
        }
    }

    @Override
    public void getNotificationInteractionResult(WindowModality windowModality, String title, String message,
            String positiveDecisionText, NotificationType notificationType) {
        AsyncTaskStatus taskStatus = getTaskStatus();

        NotificationUserInteraction interaction = InteractionFactory.eINSTANCE.createNotificationUserInteraction();
        interaction.setMessage(message);

        taskStatus.setWaitingForInteraction(interaction);

        try {
            // Wait until client acknowledges the notification
            taskStatus.waitForInteractionResponse();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for notification acknowledgement", e);
        }
    }

    @Override
    public String getTextInputInteractionResult(WindowModality windowModality, String title, String message,
            String positiveDecisionText, String cancelDecisionText, InputValidator inputValidator) {
        AsyncTaskStatus taskStatus = getTaskStatus();

        FreeTextUserInteraction interaction = InteractionFactory.eINSTANCE.createFreeTextUserInteraction();
        interaction.setMessage(message);

        taskStatus.setWaitingForInteraction(interaction);

        try {
            FreeTextUserInteraction response = taskStatus.waitForInteractionResponse();
            return response.getText();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for text input response", e);
        }
    }

    @Override
    public int getMultipleChoiceSingleSelectionInteractionResult(WindowModality windowModality, String title,
            String message, String positiveDecisionText, String cancelDecisionText, Iterable<String> choices) {
        AsyncTaskStatus taskStatus = getTaskStatus();

        MultipleChoiceSingleSelectionUserInteraction interaction = InteractionFactory.eINSTANCE
                .createMultipleChoiceSingleSelectionUserInteraction();
        interaction.setMessage(message);
        List<String> choicesList = new ArrayList<>();
        choices.forEach(choicesList::add);
        interaction.getChoices().addAll((java.util.Collection<? extends String>) choices);

        taskStatus.setWaitingForInteraction(interaction);

        try {
            MultipleChoiceSingleSelectionUserInteraction response = taskStatus.waitForInteractionResponse();
            return response.getSelectedIndex();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for single selection response", e);
        }
    }

    @Override
    public Iterable<Integer> getMultipleChoiceMultipleSelectionInteractionResult(WindowModality windowModality,
            String title, String message, String positiveDecisionText, String cancelDecisionText,
            Iterable<String> choices) {
        AsyncTaskStatus taskStatus = getTaskStatus();

        MultipleChoiceMultiSelectionUserInteraction interaction = InteractionFactory.eINSTANCE
                .createMultipleChoiceMultiSelectionUserInteraction();
        interaction.setMessage(message);
        interaction.getChoices().addAll((java.util.Collection<? extends String>) choices);

        taskStatus.setWaitingForInteraction(interaction);

        try {
            MultipleChoiceMultiSelectionUserInteraction response = taskStatus.waitForInteractionResponse();
            return response.getSelectedIndices();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for multi selection response", e);
        }
    }
}
