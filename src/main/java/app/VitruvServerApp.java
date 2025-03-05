package app;

import config.ConfigManager;
import server.HttpsServerManager;
import server.VitruvServerManager;
import oidc.OIDCClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class VitruvServerApp {

    public static final Logger logger = LoggerFactory.getLogger(VitruvServerApp.class);
    private static OIDCClient oidcClient;

    private static ConfigManager config;

    public static void main(String[] args) throws Exception {
        logger.info("Initialize client and servers");

        config = new ConfigManager("config.properties");

        final VitruvServerManager vitruvServerManager = new VitruvServerManager(config.getVitruvServerPort());
        vitruvServerManager.start();

        final HttpsServerManager httpsServerManager = new HttpsServerManager(config.getHttpsServerPort(), config.getVitruvServerPort());
        httpsServerManager.start();

        final String redirectURI = config.getDomainProtocol() + "://" + config.getDomainName() + "/callback";
        logger.debug("redirectURI: {}", redirectURI);
        oidcClient = new OIDCClient(config.getClientId(), config.getClientSecret(), redirectURI);

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