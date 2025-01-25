import config.ConfigManager;
import handler.HttpsServerManager;
import handler.VitruvServerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class VitruvServerApp {

    public static final Logger logger = LoggerFactory.getLogger(VitruvServerApp.class);

    public static void main(String[] args) throws Exception {
        final ConfigManager config = new ConfigManager("config.properties");
        final int vitruvPort = config.getVitruvServerPort();
        final int httpsPort = config.getHttpsServerPort();

        final VitruvServerManager vitruvServerManager = new VitruvServerManager(vitruvPort);
        vitruvServerManager.start();

        final HttpsServerManager httpsServerManager = new HttpsServerManager(httpsPort, vitruvPort);
        httpsServerManager.start();

        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> logger.info("still running.."), 0, 15, TimeUnit.SECONDS);
    }
}