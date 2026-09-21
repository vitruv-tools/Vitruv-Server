package tools.vitruv.framework.remote.modules.vsums.async;

import tools.vitruv.change.interaction.InteractionResultProvider;
import tools.vitruv.change.interaction.UserInteractionOptions.InputValidator;
import tools.vitruv.change.interaction.UserInteractionOptions.NotificationType;
import tools.vitruv.change.interaction.UserInteractionOptions.WindowModality;

import java.util.Collections;

/**
 * Answers Vitruv user interactions deterministically while replaying persisted view state.
 * Confirmations are declined so a previous "No" is not overridden on server restart / VSUM reload.
 */
public class StateReplayInteractionResultProvider implements InteractionResultProvider {

    @Override
    public boolean getConfirmationInteractionResult(
            WindowModality windowModality,
            String title,
            String message,
            String positiveDecisionText,
            String negativeDecisionText,
            String cancelDecisionText) {
        return false;
    }

    @Override
    public void getNotificationInteractionResult(
            WindowModality windowModality,
            String title,
            String message,
            String positiveDecisionText,
            NotificationType notificationType) {
        // acknowledgement only
    }

    @Override
    public String getTextInputInteractionResult(
            WindowModality windowModality,
            String title,
            String message,
            String positiveDecisionText,
            String cancelDecisionText,
            InputValidator inputValidator) {
        return "";
    }

    @Override
    public int getMultipleChoiceSingleSelectionInteractionResult(
            WindowModality windowModality,
            String title,
            String message,
            String positiveDecisionText,
            String cancelDecisionText,
            Iterable<String> choices) {
        return 0;
    }

    @Override
    public Iterable<Integer> getMultipleChoiceMultipleSelectionInteractionResult(
            WindowModality windowModality,
            String title,
            String message,
            String positiveDecisionText,
            String cancelDecisionText,
            Iterable<String> choices) {
        return Collections.emptyList();
    }
}
