package tools.vitruv.remote.secserver;

import org.eclipse.jetty.server.Server;

import tools.vitruv.framework.remote.server.VirtualModelInitializer;
import tools.vitruv.framework.remote.server.VitruviusServer;
import tools.vitruv.remote.secserver.config.ServerConfiguration;
import tools.vitruv.remote.secserver.jetty.JettyServerConnectionInitializer;
import tools.vitruv.remote.secserver.jetty.JettyServerHandlerInitializer;
import tools.vitruv.remote.secserver.mode.ServerModeController;
import tools.vitruv.remote.secserver.mode.ServerModeControllerFactory;

public class VitruvSecurityServer2 implements VitruviusServer {
    private ServerConfiguration config;
    private Server server;
    private ServerModeController modeController;
    private boolean isInitialized = false;

    public VitruvSecurityServer2(ServerConfiguration config) {
        this.config = config;
    }

    @Override
    public void initialize(VirtualModelInitializer modelInitializer) throws Exception {
        if (this.isInitialized) {
            return;
        }

        this.modeController = ServerModeControllerFactory.createModeController(this.config.handlerConfig());
        this.modeController.initialize(modelInitializer);
        JettyServerConnectionInitializer.initializeConnectors(this.server, this.config.connectionConfig());
        JettyServerHandlerInitializer.initializeHandlers(this.server, this.config.handlerConfig(), this.modeController.getHandler());
    }

    @Override
    public String getBaseUrl() {
        return "https://" + this.config.connectionConfig().hostName() + ":" + this.config.connectionConfig().port();
    }

    @Override
    public void start() throws Exception {
        if (!this.isInitialized) {
            throw new IllegalStateException("Server not initialized.");
        }

        this.modeController.start();
        this.server.start();
    }

    @Override
    public void stop() throws Exception {
        if (!this.isInitialized) {
            throw new IllegalStateException("Server not initialized.");
        }

        this.modeController.stop();
        this.server.stop();
    }
}
