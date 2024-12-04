import org.eclipse.xtext.xbase.lib.Functions;
import tools.vitruv.change.interaction.InteractionResultProvider;
import tools.vitruv.change.interaction.InternalUserInteractor;
import tools.vitruv.change.interaction.UserInteractionListener;
import tools.vitruv.change.interaction.builder.*;
import tools.vitruv.framework.remote.server.VitruvServer;
import tools.vitruv.framework.vsum.VirtualModelBuilder;

import java.io.IOException;
import java.nio.file.Path;

import static tools.vitruv.framework.views.ViewTypeFactory.createIdentityMappingViewType;

public class VitruvServerApp {

    public static void main(String[] args) throws IOException {
        System.out.println("Server startet...");

        VitruvServer server = new VitruvServer(() -> {
            VirtualModelBuilder vsum = new VirtualModelBuilder();

            /* init vsum here */
            Path pathDir = Path.of("StorageFolder");
            vsum.withStorageFolder(pathDir);

            InternalUserInteractor userInteractor = getInternalUserInteractor();
            vsum.withUserInteractor(userInteractor);

            // testing
            vsum.withViewType(createIdentityMappingViewType("MyViewTypeBob"));

            return vsum.buildAndInitialize();
        });
        server.start();


        System.out.println("Vitruv Server gestartet!");
    }


    // TODO
    private static InternalUserInteractor getInternalUserInteractor() {
        return new InternalUserInteractor() {
            @Override
            public NotificationInteractionBuilder getNotificationDialogBuilder() {
                System.out.println("xx");
                return null;
            }

            @Override
            public ConfirmationInteractionBuilder getConfirmationDialogBuilder() {
                System.out.println("xx");
                return null;
            }

            @Override
            public TextInputInteractionBuilder getTextInputDialogBuilder() {
                System.out.println("xx");
                return null;
            }

            @Override
            public MultipleChoiceSingleSelectionInteractionBuilder getSingleSelectionDialogBuilder() {
                System.out.println("xx");
                return null;
            }

            @Override
            public MultipleChoiceMultiSelectionInteractionBuilder getMultiSelectionDialogBuilder() {
                System.out.println("xx");
                return null;
            }

            @Override
            public void registerUserInputListener(UserInteractionListener userInteractionListener) {
                System.out.println("xx");
            }

            @Override
            public void deregisterUserInputListener(UserInteractionListener userInteractionListener) {
                System.out.println("xx");
            }

            @Override
            public AutoCloseable replaceUserInteractionResultProvider(Functions.Function1<? super InteractionResultProvider, ? extends InteractionResultProvider> function1) {
                System.out.println("xx");
                return null;
            }
        };
    }
}
