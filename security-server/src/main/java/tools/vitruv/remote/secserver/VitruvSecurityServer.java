package tools.vitruv.remote.secserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.vitruv.framework.remote.server.VirtualModelInitializer;
import tools.vitruv.framework.remote.server.VitruvServer;
import tools.vitruv.framework.remote.server.VitruviusServer;
import tools.vitruv.remote.secserver.config.ConfigManager;
import tools.vitruv.remote.secserver.oidc.OIDCClient;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A {@link VitruviusServer} implementation, which wraps a {@link VitruvServer} behind a proxy server that
 * adds TLS, authentication and authorization.
 */
public class VitruvSecurityServer implements VitruviusServer {
    public static final Logger logger = LoggerFactory.getLogger(VitruvSecurityServer.class);
    private VitruvServer vitruvServer;
    private SecurityServerManager securityServerManager;
    private String baseUrl;

    @Override
    public void initialize(VirtualModelInitializer modelInitializer, int port, String hostOrIp) throws Exception {
        logger.info("Starting initialization of servers and OIDC client...");

        ConfigManager config = new ConfigManager();

        vitruvServer = new VitruvServer();
        vitruvServer.initialize(modelInitializer, config.getVitruvServerPort(), "localhost");

        baseUrl = "https://" + config.getDomainName() + ":" + config.getHttpsServerPort();
        final String redirectURI = baseUrl + "/callback";
        logger.debug("redirectURI: {}", redirectURI);
        OIDCClient oidcClient = new OIDCClient(config.getOidcClientId(), config.getOidcClientSecret(), redirectURI);
        
        securityServerManager =
                new SecurityServerManager(config.getHttpsServerPort(), config.getVitruvServerPort(), config.getTlsPassword(), oidcClient, config);

        logger.info("Initialization completed.");

        // Periodic server notification (can be omitted)
        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> logger.info("Running"), 0, 1, TimeUnit.DAYS);
    }

    @Override
    public String getBaseUrl() {
        return this.baseUrl;
    }

    @Override
    public void start() {
        vitruvServer.start();
        securityServerManager.start();
    }

    @Override
    public void stop() {
        securityServerManager.stop();
        vitruvServer.stop();
    }
}