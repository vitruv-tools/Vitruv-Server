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

    public static void main(String[] args) throws Exception {
        logger.info("Initialize client and servers");

        final ConfigManager config = new ConfigManager("config.properties");
        final int vitruvPort = config.getVitruvServerPort();
        final int httpsPort = config.getHttpsServerPort();

        final VitruvServerManager vitruvServerManager = new VitruvServerManager(vitruvPort);
        vitruvServerManager.start();

        final HttpsServerManager httpsServerManager = new HttpsServerManager(httpsPort, vitruvPort);
        httpsServerManager.start();

        oidcClient = new OIDCClient(config.getClientId(), config.getClientSecret(), "https://localhost:8443/callback");

        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> logger.info("still running.."), 0, 1, TimeUnit.MINUTES);
    }

    public static OIDCClient getOidcClient() {
        return oidcClient;
    }
}