package tools.vitruv.framework.remote.modules.vsums.async;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.vitruv.change.interaction.InteractionResultProvider;
import tools.vitruv.change.interaction.UserInteractionOptions.InputValidator;
import tools.vitruv.change.interaction.UserInteractionOptions.NotificationType;
import tools.vitruv.change.interaction.UserInteractionOptions.WindowModality;
import tools.vitruv.change.testutils.TestUserInteraction;

/**
 * Injected into VSUMs at load time. Uses {@link ServerInteractionResultProvider} during async
 * propagation (when a task id is set on the worker thread) and auto-confirming test interactions
 * otherwise, preserving existing synchronous behaviour.
 */
@Component
@RequiredArgsConstructor
public class VitruviusInteractionResultProvider implements InteractionResultProvider {

  private final ServerInteractionResultProvider asyncProvider;
  private final PropagationTaskRegistry taskRegistry;
  private final InteractionResultProvider syncProvider =
      new TestUserInteraction.ResultProvider(new TestUserInteraction());
  private final InteractionResultProvider replayProvider = new StateReplayInteractionResultProvider();

  @Override
  public boolean getConfirmationInteractionResult(
      WindowModality windowModality,
      String title,
      String message,
      String positiveDecisionText,
      String negativeDecisionText,
      String cancelDecisionText) {
    return delegate().getConfirmationInteractionResult(
        windowModality,
        title,
        message,
        positiveDecisionText,
        negativeDecisionText,
        cancelDecisionText);
  }

  @Override
  public void getNotificationInteractionResult(
      WindowModality windowModality,
      String title,
      String message,
      String positiveDecisionText,
      NotificationType notificationType) {
    delegate().getNotificationInteractionResult(
        windowModality, title, message, positiveDecisionText, notificationType);
  }

  @Override
  public String getTextInputInteractionResult(
      WindowModality windowModality,
      String title,
      String message,
      String positiveDecisionText,
      String cancelDecisionText,
      InputValidator inputValidator) {
    return delegate().getTextInputInteractionResult(
        windowModality,
        title,
        message,
        positiveDecisionText,
        cancelDecisionText,
        inputValidator);
  }

  @Override
  public int getMultipleChoiceSingleSelectionInteractionResult(
      WindowModality windowModality,
      String title,
      String message,
      String positiveDecisionText,
      String cancelDecisionText,
      Iterable<String> choices) {
    return delegate().getMultipleChoiceSingleSelectionInteractionResult(
        windowModality, title, message, positiveDecisionText, cancelDecisionText, choices);
  }

  @Override
  public Iterable<Integer> getMultipleChoiceMultipleSelectionInteractionResult(
      WindowModality windowModality,
      String title,
      String message,
      String positiveDecisionText,
      String cancelDecisionText,
      Iterable<String> choices) {
    return delegate().getMultipleChoiceMultipleSelectionInteractionResult(
        windowModality, title, message, positiveDecisionText, cancelDecisionText, choices);
  }

  private InteractionResultProvider delegate() {
    if (InteractionContext.isStateReplay()) {
      return replayProvider;
    }
    if (ServerInteractionResultProvider.isAsyncContext() || taskRegistry.hasActivePropagationTask()) {
      return asyncProvider;
    }
    return syncProvider;
  }
}
