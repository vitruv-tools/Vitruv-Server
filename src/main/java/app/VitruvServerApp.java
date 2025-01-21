package app;

import config.ConfigManager;
import handler.HttpsServerManager;
import handler.VitruvServerManager;
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
        final ConfigManager config = new ConfigManager("config.properties");
        final int vitruvPort = config.getVitruvServerPort();
        final int httpsPort = config.getHttpsServerPort();

        oidcClient = new OIDCClient("vitruvserver-maven-dev", "A5MqhxujnpAQC0zzN0BW5pZKQ5t27C8P", "https://localhost/callback");

        final VitruvServerManager vitruvServerManager = new VitruvServerManager(vitruvPort);
        vitruvServerManager.start();

        final HttpsServerManager httpsServerManager = new HttpsServerManager(httpsPort, vitruvPort);
        httpsServerManager.start();

        logger.info("Authorization URL: {}", oidcClient.getAuthorizationRequestURI());

        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> logger.info("still running.."), 0, 20, TimeUnit.SECONDS);
    }

    public static OIDCClient getOidcClient() {
        return oidcClient;
    }
}