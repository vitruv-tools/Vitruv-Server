package tools.vitruv.framework.remote.server.async;

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
 * InteractionResultProvider that captures UserInteractions from Vitruv
 * reactions
 * and stores them in the AsyncTaskStatus, allowing the client to be notified
 * and provide answers asynchronously via REST endpoint.
 */
public class AsyncInteractionResultProvider implements InteractionResultProvider {
    private final String taskId;
    private final AsyncTaskStatus taskStatus;

    public AsyncInteractionResultProvider(String taskId) {
        this.taskId = taskId;
        var status = AsyncTaskRegistry.getInstance().getTaskStatus(taskId);
        if (status == null) {
            throw new RuntimeException("Task not found: " + taskId);
        }
        this.taskStatus = status;
    }

    @Override
    public boolean getConfirmationInteractionResult(WindowModality windowModality, String title, String message,
            String positiveDecisionText, String negativeDecisionText, String cancelDecisionText) {
        // Create a ConfirmationUserInteraction object with all dialog information
        ConfirmationUserInteraction interaction = InteractionFactory.eINSTANCE.createConfirmationUserInteraction();
        interaction.setMessage(message);

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
    public void getNotificationInteractionResult(WindowModality windowModality, String title,
            String message, String positiveDecisionText, NotificationType notificationType) {
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
        MultipleChoiceSingleSelectionUserInteraction interaction = InteractionFactory.eINSTANCE
                .createMultipleChoiceSingleSelectionUserInteraction();
        interaction.setMessage(message);
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
