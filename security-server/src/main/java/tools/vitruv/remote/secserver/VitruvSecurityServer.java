package tools.vitruv.remote.secserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.vitruv.framework.remote.common.DefaultConnectionSettings;
import tools.vitruv.framework.remote.server.VirtualModelInitializer;
import tools.vitruv.framework.remote.server.VitruvServer;
import tools.vitruv.framework.remote.server.VitruvServerConfiguration;
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
    private ConfigManager configManager;

    public VitruvSecurityServer() {}

    public VitruvSecurityServer(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public void initialize(VirtualModelInitializer modelInitializer) throws Exception {
        logger.info("Starting initialization of servers and OIDC client...");

        if (this.configManager == null) {
            this.configManager = new ConfigManager();
        }

        vitruvServer = new VitruvServer(new VitruvServerConfiguration(DefaultConnectionSettings.STD_HOST, this.configManager.getVitruvServerPort()));
        vitruvServer.initialize(modelInitializer);

        baseUrl = "https://" + this.configManager.getDomainName() + ":" + this.configManager.getHttpsServerPort();
        final String redirectURI = baseUrl + "/callback";
        logger.debug("redirectURI: {}", redirectURI);
        OIDCClient oidcClient = new OIDCClient(this.configManager.getOidcClientId(), this.configManager.getOidcClientSecret(), this.configManager.getOIDCDiscoveryUri(), redirectURI);
        
        securityServerManager =
                new SecurityServerManager(this.configManager.getHttpsServerPort(), this.configManager.getVitruvServerPort(), this.configManager.getTlsPassword(), oidcClient, this.configManager);

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
    public void start() throws Exception {
        vitruvServer.start();
        securityServerManager.start();
    }

    @Override
    public void stop() throws Exception {
        securityServerManager.stop();
        vitruvServer.stop();
    }
}