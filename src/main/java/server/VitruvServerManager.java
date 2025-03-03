package server;

import config.UserInteractorManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.vitruv.framework.remote.server.VitruvServer;
import tools.vitruv.framework.views.ViewTypeFactory;
import tools.vitruv.framework.vsum.VirtualModelBuilder;

import java.nio.file.Path;

import static tools.vitruv.framework.views.ViewTypeFactory.createIdentityMappingViewType;

public class VitruvServerManager {

    private static final Logger logger = LoggerFactory.getLogger(VitruvServerManager.class);
    /**
     * Default name of the storage folder containing vitruv specific files.
     */
    private static final String DEFAULT_STORAGE_FOLDER_NAME = "StorageFolder";

    private final int port;
    private VitruvServer server;

    public VitruvServerManager(int port) {
        this.port = port;
    }

    public void start() throws Exception {
        server = new VitruvServer(() -> {
            final VirtualModelBuilder vsumBuilder = new VirtualModelBuilder();

            /////////////////////////////////////////////////////////////////////////////////
            // TODO: init vsumBuilder here (testing area)
            final Path pathDir = Path.of(DEFAULT_STORAGE_FOLDER_NAME);
            vsumBuilder.withStorageFolder(pathDir);

            vsumBuilder.withUserInteractor(UserInteractorManager.createInternalUserInteractor());

            vsumBuilder.withViewType(ViewTypeFactory.createIdentityMappingViewType("DefaultView"));
            vsumBuilder.withViewType(createIdentityMappingViewType("MyViewTypeBob17"));
            vsumBuilder.withViewType(createIdentityMappingViewType("MyViewTypeBob18"));
            /////////////////////////////////////////////////////////////////////////////////

            return vsumBuilder.buildAndInitialize();
        }, port);
        server.start();
        logger.info("VitruvServer started on port " + port);
    }
}