package config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.vitruv.change.interaction.InteractionResultProvider;
import tools.vitruv.change.interaction.InternalUserInteractor;
import tools.vitruv.change.interaction.UserInteractionListener;
import tools.vitruv.change.interaction.builder.*;
import org.eclipse.xtext.xbase.lib.Functions;

public class UserInteractorManager {

    private static final Logger logger = LoggerFactory.getLogger(UserInteractorManager.class);

    public static InternalUserInteractor createInternalUserInteractor() {
        return new InternalUserInteractor() {
            @Override
            public NotificationInteractionBuilder getNotificationDialogBuilder() {
                logger.warn("getNotificationDialogBuilder() is not implemented.");
                return null;
            }

            @Override
            public ConfirmationInteractionBuilder getConfirmationDialogBuilder() {
                logger.warn("getConfirmationDialogBuilder() is not implemented.");
                return null;
            }

            @Override
            public TextInputInteractionBuilder getTextInputDialogBuilder() {
                logger.warn("getTextInputDialogBuilder() is not implemented.");
                return null;
            }

            @Override
            public MultipleChoiceSingleSelectionInteractionBuilder getSingleSelectionDialogBuilder() {
                logger.warn("getSingleSelectionDialogBuilder() is not implemented.");
                return null;
            }

            @Override
            public MultipleChoiceMultiSelectionInteractionBuilder getMultiSelectionDialogBuilder() {
                logger.warn("getMultiSelectionDialogBuilder() is not implemented.");
                return null;
            }

            @Override
            public void registerUserInputListener(UserInteractionListener userInteractionListener) {
                logger.warn("registerUserInputListener() is not implemented.");
            }

            @Override
            public void deregisterUserInputListener(UserInteractionListener userInteractionListener) {
                logger.warn("deregisterUserInputListener() is not implemented.");
            }

            @Override
            public AutoCloseable replaceUserInteractionResultProvider(Functions.Function1<? super InteractionResultProvider, ? extends InteractionResultProvider> function1) {
                logger.warn("replaceUserInteractionResultProvider() is not implemented.");
                return null;
            }
        };
    }
}
