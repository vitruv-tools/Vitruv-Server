package app;

import config.ConfigManager;
import oidc.OIDCClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.SecurityServerManager;
import server.VitruvServerManager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class VitruvSecurityServerApp {

    public static final Logger logger = LoggerFactory.getLogger(VitruvSecurityServerApp.class);
    private static OIDCClient oidcClient;
    private static ConfigManager config;

    public static void main(String[] args) throws Exception {
        logger.info("Starting initialization of servers and OIDC client...");

        config = new ConfigManager();

        final VitruvServerManager vitruvServerManager = new VitruvServerManager(config.getVitruvServerPort());
        vitruvServerManager.start();

        final SecurityServerManager securityServerManager =
                new SecurityServerManager(config.getHttpsServerPort(), config.getVitruvServerPort(), config.getTlsPassword());
        securityServerManager.start();

        final String redirectURI = config.getDomainProtocol() + "://" + config.getDomainName() + "/callback";
        logger.debug("redirectURI: {}", redirectURI);
        oidcClient = new OIDCClient(config.getOidcClientId(), config.getOidcClientSecret(), redirectURI);

        logger.info("Initialization completed.");

        // Periodic server notification (can be omitted)
        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> logger.info("still running.."), 0, 1, TimeUnit.DAYS);
    }

    public static OIDCClient getOidcClient() {
        return oidcClient;
    }
    public static ConfigManager getServerConfig() {
        return config;
    }
}