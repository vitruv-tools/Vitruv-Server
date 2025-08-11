package tools.vitruv.remote.secserver;

import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;
import org.eclipse.jetty.server.CustomRequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.Slf4jRequestLogWriter;

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

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.insertProviderAt(new BouncyCastleProvider(), 1);
        }
        if (Security.getProvider(BouncyCastleJsseProvider.PROVIDER_NAME) == null) {
            Security.insertProviderAt(new BouncyCastleJsseProvider(), 2);
        }
    }

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
        this.server = new Server();
        this.server.setRequestLog(new CustomRequestLog(new Slf4jRequestLogWriter(), CustomRequestLog.EXTENDED_NCSA_FORMAT));
        JettyServerConnectionInitializer.initializeConnectors(this.server, this.config.connectionConfig());
        JettyServerHandlerInitializer.initializeHandlers(this.server, this.config.handlerConfig(), this.modeController.getHandler());
        this.isInitialized = true;
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
