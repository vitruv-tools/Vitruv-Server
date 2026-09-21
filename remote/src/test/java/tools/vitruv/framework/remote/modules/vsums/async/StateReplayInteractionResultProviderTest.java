package tools.vitruv.framework.remote.modules.vsums.async;

import org.junit.jupiter.api.Test;
import tools.vitruv.change.interaction.UserInteractionOptions.NotificationType;
import tools.vitruv.change.interaction.UserInteractionOptions.WindowModality;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * The replay provider must answer deterministically while a persisted view is reloaded,
 * so a previous "No" is never silently overridden on server restart / VSUM reload.
 */
class StateReplayInteractionResultProviderTest {

    private final StateReplayInteractionResultProvider provider = new StateReplayInteractionResultProvider();

    @Test
    void declinesConfirmations() {
        boolean result = provider.getConfirmationInteractionResult(
                WindowModality.MODAL, "title", "Proceed?", "Yes", "No", "Cancel");
        assertThat(result).isFalse();
    }

    @Test
    void returnsEmptyTextInput() {
        String result = provider.getTextInputInteractionResult(
                WindowModality.MODAL, "title", "Enter value", "Ok", "Cancel", null);
        assertThat(result).isEmpty();
    }

    @Test
    void selectsFirstChoiceForSingleSelection() {
        int index = provider.getMultipleChoiceSingleSelectionInteractionResult(
                WindowModality.MODAL, "title", "Pick one", "Ok", "Cancel", List.of("a", "b", "c"));
        assertThat(index).isZero();
    }

    @Test
    void selectsNothingForMultiSelection() {
        Iterable<Integer> selected = provider.getMultipleChoiceMultipleSelectionInteractionResult(
                WindowModality.MODAL, "title", "Pick some", "Ok", "Cancel", List.of("a", "b"));
        assertThat(selected).isEmpty();
    }

    @Test
    void acknowledgesNotificationsWithoutThrowing() {
        assertThatCode(() -> provider.getNotificationInteractionResult(
                WindowModality.MODAL, "title", "Heads up", "Ok", NotificationType.INFORMATION))
                .doesNotThrowAnyException();
    }
}
