package tools.vitruv.framework.remote.modules.vsums.async;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.vitruv.change.interaction.*;
import tools.vitruv.change.interaction.UserInteractionOptions.InputValidator;
import tools.vitruv.change.interaction.UserInteractionOptions.NotificationType;
import tools.vitruv.change.interaction.UserInteractionOptions.WindowModality;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Bridges Vitruv user interactions to the async propagation task registry.
 * Used during async {@code view.update()} so the client can respond via REST.
 */
@Component
@RequiredArgsConstructor
public class ServerInteractionResultProvider implements InteractionResultProvider {

  private static final ThreadLocal<UUID> CURRENT_TASK_ID = new ThreadLocal<>();

  private final PropagationTaskRegistry taskRegistry;

  public static void setCurrentTaskId(UUID taskId) {
    CURRENT_TASK_ID.set(taskId);
  }

  public static void clearCurrentTaskId() {
    CURRENT_TASK_ID.remove();
  }

  public static Optional<UUID> getCurrentTaskId() {
    return Optional.ofNullable(CURRENT_TASK_ID.get());
  }

  public static boolean isAsyncContext() {
    return CURRENT_TASK_ID.get() != null;
  }

  private AsyncTaskStatus getTaskStatus() {
    UUID taskId = taskRegistry.resolveActiveTaskId().orElse(null);
    if (taskId == null) {
      throw new UnsupportedOperationException(
          "User interactions are not supported in synchronous change propagation mode. "
              + "Use async propagation endpoint instead.");
    }

    AsyncTaskStatus taskStatus = taskRegistry.getTaskStatus(taskId);
    if (taskStatus == null) {
      throw new IllegalStateException("Task not found in registry: " + taskId);
    }
    return taskStatus;
  }

  @Override
  public boolean getConfirmationInteractionResult(
      WindowModality windowModality,
      String title,
      String message,
      String positiveDecisionText,
      String negativeDecisionText,
      String cancelDecisionText) {
    AsyncTaskStatus taskStatus = getTaskStatus();

    ConfirmationUserInteraction interaction = InteractionFactory.eINSTANCE.createConfirmationUserInteraction();
    interaction.setMessage(message);
    taskStatus.setWaitingForInteraction(interaction);

    try {
      ConfirmationUserInteraction response = taskStatus.waitForInteractionResponse();
      return response.isConfirmed();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Interrupted while waiting for confirmation response", e);
    }
  }

  @Override
  public void getNotificationInteractionResult(
      WindowModality windowModality,
      String title,
      String message,
      String positiveDecisionText,
      NotificationType notificationType) {
    AsyncTaskStatus taskStatus = getTaskStatus();

    NotificationUserInteraction interaction = InteractionFactory.eINSTANCE.createNotificationUserInteraction();
    interaction.setMessage(message);
    taskStatus.setWaitingForInteraction(interaction);

    try {
      taskStatus.waitForInteractionResponse();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Interrupted while waiting for notification acknowledgement", e);
    }
  }

  @Override
  public String getTextInputInteractionResult(
      WindowModality windowModality,
      String title,
      String message,
      String positiveDecisionText,
      String cancelDecisionText,
      InputValidator inputValidator) {
    AsyncTaskStatus taskStatus = getTaskStatus();
    String promptMessage = message;
    taskStatus.clearValidationError();

    while (true) {
      FreeTextUserInteraction interaction = InteractionFactory.eINSTANCE.createFreeTextUserInteraction();
      interaction.setMessage(promptMessage);
      taskStatus.setWaitingForInteraction(interaction);

      try {
        FreeTextUserInteraction response = taskStatus.waitForInteractionResponse();
        String text = response.getText() != null ? response.getText() : "";

        if (inputValidator == null || inputValidator.isInputValid(text)) {
          taskStatus.clearValidationError();
          return text;
        }

        String errorMessage = inputValidator.getInvalidInputMessage(text);
        if (errorMessage == null || errorMessage.isBlank()) {
          errorMessage = "Invalid input. Please try again.";
        }
        taskStatus.setValidationError(errorMessage);
        // Keep original prompt visible; frontend shows validationError separately.
        promptMessage = message;
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException("Interrupted while waiting for text input response", e);
      }
    }
  }

  @Override
  public int getMultipleChoiceSingleSelectionInteractionResult(
      WindowModality windowModality,
      String title,
      String message,
      String positiveDecisionText,
      String cancelDecisionText,
      Iterable<String> choices) {
    AsyncTaskStatus taskStatus = getTaskStatus();

    MultipleChoiceSingleSelectionUserInteraction interaction =
        InteractionFactory.eINSTANCE.createMultipleChoiceSingleSelectionUserInteraction();
    interaction.setMessage(message);
    List<String> choicesList = new ArrayList<>();
    choices.forEach(choicesList::add);
    interaction.getChoices().addAll(choicesList);
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
  public Iterable<Integer> getMultipleChoiceMultipleSelectionInteractionResult(
      WindowModality windowModality,
      String title,
      String message,
      String positiveDecisionText,
      String cancelDecisionText,
      Iterable<String> choices) {
    AsyncTaskStatus taskStatus = getTaskStatus();

    MultipleChoiceMultiSelectionUserInteraction interaction =
        InteractionFactory.eINSTANCE.createMultipleChoiceMultiSelectionUserInteraction();
    interaction.setMessage(message);
    List<String> choicesList = new ArrayList<>();
    choices.forEach(choicesList::add);
    interaction.getChoices().addAll(choicesList);
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
