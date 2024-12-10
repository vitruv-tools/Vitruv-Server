import org.eclipse.xtext.xbase.lib.Functions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.vitruv.change.interaction.InteractionResultProvider;
import tools.vitruv.change.interaction.InternalUserInteractor;
import tools.vitruv.change.interaction.UserInteractionListener;
import tools.vitruv.change.interaction.builder.*;
import tools.vitruv.framework.remote.server.VitruvServer;
import tools.vitruv.framework.vsum.VirtualModelBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Optional;
import java.util.Properties;

import static tools.vitruv.framework.views.ViewTypeFactory.createIdentityMappingViewType;

public class VitruvServerApp {
    private static final int DEFAULT_PORT = 8080;
    private static final String DEFAULT_STORAGE_FOLDER_NAME = "StorageFolder";
    private static final String DEFAULT_CONFIG_PROPERTIES_NAME = "config.properties";
    private static final Logger logger = LoggerFactory.getLogger(VitruvServerApp.class);
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public static void main(String[] args) throws IOException {
        logger.info("Starting the server...");
        System.out.println("Starting the server..."); // TODO: delete


        final int port = loadPortFromConfig().orElse(DEFAULT_PORT);

        VitruvServer server = new VitruvServer(() -> {
            VirtualModelBuilder vsum = new VirtualModelBuilder();

            /* init vsum here */
            Path pathDir = Path.of(DEFAULT_STORAGE_FOLDER_NAME);
            vsum.withStorageFolder(pathDir);

            InternalUserInteractor userInteractor = getInternalUserInteractor();
            vsum.withUserInteractor(userInteractor);

            // TODO: remove following test code
            vsum.withViewType(createIdentityMappingViewType("MyViewTypeBob"));
            ////

            return vsum.buildAndInitialize();
        }, port);
        server.start();

        System.out.println("Server started on port " + port + "."); // TODO: delete
        logger.info("Server started on port " + port + ". ");
        scheduler.scheduleAtFixedRate(() -> logger.info("still running"), 0, 5, TimeUnit.SECONDS);
    }

    private static Optional<Integer> loadPortFromConfig() {
        try (final InputStream inputStream = VitruvServerApp.class.getClassLoader().getResourceAsStream(DEFAULT_CONFIG_PROPERTIES_NAME)) {
            if (inputStream != null) {
                final Properties properties = new Properties();
                properties.load(inputStream);
                String portStr = properties.getProperty("server.port");
                if (portStr != null) {
                    return Optional.of(Integer.parseInt(portStr));
                }
            }
        } catch (Exception err) {
            logger.error("Could not read " + DEFAULT_CONFIG_PROPERTIES_NAME + ". Error message: {}", err.getMessage(), err);
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
