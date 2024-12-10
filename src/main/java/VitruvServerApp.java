import org.eclipse.xtext.xbase.lib.Functions;
import tools.vitruv.change.interaction.InteractionResultProvider;
import tools.vitruv.change.interaction.InternalUserInteractor;
import tools.vitruv.change.interaction.UserInteractionListener;
import tools.vitruv.change.interaction.builder.*;
import tools.vitruv.framework.remote.server.VitruvServer;
import tools.vitruv.framework.vsum.VirtualModelBuilder;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

import static tools.vitruv.framework.views.ViewTypeFactory.createIdentityMappingViewType;

public class VitruvServerApp {
    private static final int DEFAULT_PORT = 8080;
    private static final String DEFAULT_CONFIG_PROPERTIES_NAME = "config.properties";


    public static void main(String[] args) throws IOException {
        final int port = loadPortFromConfig().orElse(DEFAULT_PORT);

        System.out.println("Starting the server...");

        VitruvServer server = new VitruvServer(() -> {
            VirtualModelBuilder vsum = new VirtualModelBuilder();

            /* init vsum here */
            Path pathDir = Path.of("StorageFolder");
            vsum.withStorageFolder(pathDir);

            InternalUserInteractor userInteractor = getInternalUserInteractor();
            vsum.withUserInteractor(userInteractor);

            // TODO: remove following test code
            vsum.withViewType(createIdentityMappingViewType("MyViewTypeBob"));

            return vsum.buildAndInitialize();
        }, port);
        server.start();

        System.out.println("Server started on port " + port + ".");
    }

    private static Optional<Integer> loadPortFromConfig() {
        try (final var inputStream = VitruvServerApp.class.getClassLoader().getResourceAsStream(DEFAULT_CONFIG_PROPERTIES_NAME)) {
            if (inputStream != null) {
                final var properties = new Properties();
                properties.load(inputStream);
                String portStr = properties.getProperty("server.port");
                if (portStr != null) {
                    return Optional.of(Integer.parseInt(portStr));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.empty();
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
