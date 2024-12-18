import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import org.eclipse.xtext.xbase.lib.Functions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.vitruv.change.interaction.InteractionResultProvider;
import tools.vitruv.change.interaction.InternalUserInteractor;
import tools.vitruv.change.interaction.UserInteractionListener;
import tools.vitruv.change.interaction.builder.*;
import tools.vitruv.framework.remote.server.VitruvServer;
import tools.vitruv.framework.vsum.VirtualModelBuilder;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.file.Path;
import java.security.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static tools.vitruv.framework.views.ViewTypeFactory.createIdentityMappingViewType;

public class VitruvServerApp {
    /**
     * Fallback value of the vitruv server port.
     * The desired port should be configured in the properties file located at src/main/resources/config.properties
     */
    private static final int FALLBACK_VITRUV_SERVER_PORT = 8080;
    /**
     * Fallback value of the HTTPS server port.
     * The desired port should be configured in the properties file located at src/main/resources/config.properties
     */
    private static final int FALLBACK_HTTPS_PORT = 8443;
    /**
     * Default name of the storage folder containing vitruv specific files.
     */
    private static final String DEFAULT_STORAGE_FOLDER_NAME = "StorageFolder";
    /**
     * Default name of the configuration file containing the server ports.
     */
    private static final String DEFAULT_CONFIG_PROPERTIES_NAME = "config.properties";

    /**
     * Default name of the keystore file containing the self-signed certificate.
     */
    private static final String DEFAULT_KEYSTORE_NAME = "keystore.p12";
    /**
     * Default password for the keystore containing the self-signed certificate.
     */
    private static final String DEFAULT_KEYSTORE_PASSWORD = "password";

    private static final Map<String, Integer> ports = loadPortsFromConfig();
    /**
     * The SLF4J logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(VitruvServerApp.class);

    public static void main(String[] args) throws Exception {
        System.out.println("App started");
        logger.info("Starting the server...");

        final int vitruvServerPort = ports.getOrDefault("vitruv-server.port", FALLBACK_VITRUV_SERVER_PORT);
        final int httpsServerPort = ports.getOrDefault("https-server.port", FALLBACK_HTTPS_PORT);

        startVitruvServer(vitruvServerPort);
        startHTTPSServer(httpsServerPort, vitruvServerPort);

        logger.info("HTTPS server started on port " + httpsServerPort + ".");
        logger.info("Vitruv server started on port " + vitruvServerPort + ".");

        // check if servers are still running
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> logger.info("still running.."), 0, 10, TimeUnit.SECONDS);
    }

    private static void startVitruvServer(int vitruvServerPort) throws IOException {
        VitruvServer vitruvServer = new VitruvServer(() -> {
            VirtualModelBuilder vsum = new VirtualModelBuilder();

            /////////////////////////////////////////////////////////////////////////////////
            // TODO: init vsum here (testing area)
            Path pathDir = Path.of(DEFAULT_STORAGE_FOLDER_NAME);
            vsum.withStorageFolder(pathDir);

            InternalUserInteractor userInteractor = getInternalUserInteractor();
            vsum.withUserInteractor(userInteractor);

            vsum.withViewType(createIdentityMappingViewType("MyViewTypeBob17"));
            /////////////////////////////////////////////////////////////////////////////////

            return vsum.buildAndInitialize();
        }, vitruvServerPort);
        vitruvServer.start();
    }

    private static void startHTTPSServer(final int httpsServerPort, final int vitruvServerPort) throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException, KeyManagementException, IOException {
        // prepare HTTPS server (with self-signed certificate)
        SSLContext sslContext = SSLContext.getInstance("TLS");
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (final InputStream inputStream = VitruvServerApp.class.getClassLoader().getResourceAsStream(DEFAULT_KEYSTORE_NAME)) {
            assert inputStream != null;
            keyStore.load(inputStream, DEFAULT_KEYSTORE_PASSWORD.toCharArray());
        } catch (Exception e) {
            logger.error("Could not read " + DEFAULT_KEYSTORE_NAME + ". Error message: {}", e.getMessage(), e);
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(keyStore, DEFAULT_KEYSTORE_PASSWORD.toCharArray());
        sslContext.init(kmf.getKeyManagers(), null, null);

        HttpsServer httpsServer = HttpsServer.create(new InetSocketAddress(httpsServerPort), 0);
        httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
            @Override
            public void configure(HttpsParameters params) {
                try {
                    SSLContext context = getSSLContext();
                    SSLEngine engine = context.createSSLEngine();
                    params.setNeedClientAuth(false);
                    params.setCipherSuites(engine.getEnabledCipherSuites());
                    params.setProtocols(engine.getEnabledProtocols());
                    params.setSSLParameters(context.getDefaultSSLParameters());
                } catch (Exception e) {
                    logger.error("Failed to configure HTTPS: " + e.getMessage());
                    throw new RuntimeException(e);
                }
            }
        });

        // Forward HTTP requests to VitruvServer
        httpsServer.createContext("/", ex -> handleRequest(ex, vitruvServerPort));

        // start https server
        httpsServer.setExecutor(Executors.newFixedThreadPool(10));
        httpsServer.start();
    }

    /**
     * Handles an HTTPS request by forwarding it to the internal HTTP VitruvServer.
     *
     * @param exchange the HTTP exchange object containing the request and response details
     * @param vitruvServerPort     the port number of the internal VitruvServer
     * @throws IOException if an I/O error occurs while processing the request
     */
    private static void handleRequest(HttpExchange exchange, int vitruvServerPort) throws IOException {
        logger.info("redirect to VitruvServer at port {}", vitruvServerPort);
        logger.info("Request URI: {}", exchange.getRequestURI().toString());

        // connect to intern http VitruvServer
        String vitruvHost = "http://localhost:" + vitruvServerPort; // TODO: configure domain
        String fullUri = vitruvHost + exchange.getRequestURI().toString();

        // redirect HTTP request to Vitruv
        HttpURLConnection connection = (HttpURLConnection) new URL(fullUri).openConnection();
        connection.setRequestMethod(exchange.getRequestMethod());
        exchange.getRequestHeaders().forEach((key, values) -> {
            for (String value : values) {
                connection.setRequestProperty(key, value);
            }
        });

        // redirect body
        if (exchange.getRequestBody().available() > 0) {
            connection.setDoOutput(true);
            try (OutputStream os = connection.getOutputStream()) {
                os.write(exchange.getRequestBody().readAllBytes());
            }
        }

        // read answer
        int responseCode = connection.getResponseCode();
        InputStream responseStream = responseCode >= 400
                ? connection.getErrorStream()
                : connection.getInputStream();

        // return answer to client
        exchange.sendResponseHeaders(responseCode, responseStream.available());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseStream.readAllBytes());
        }
        logger.info("Response code: {}", responseCode);
    }

    /**
     * Loads the server port configuration for both the VitruvServer and the HTTPS server
     * from the default properties file.
     *
     * @return a Map containing the port numbers with keys "vitruv-server.port" and "https-server.port",
     * or an empty Map if the properties cannot be read.
     */
    private static Map<String, Integer> loadPortsFromConfig() {
        Map<String, Integer> ports = new HashMap<>();
        try (final InputStream inputStream =
                     VitruvServerApp.class.getClassLoader().getResourceAsStream(DEFAULT_CONFIG_PROPERTIES_NAME)) {
            if (inputStream != null) {
                final Properties properties = new Properties();
                properties.load(inputStream);

                // Load VitruvServer port
                String vitruvPortStr = properties.getProperty("vitruv-server.port");
                if (vitruvPortStr != null) {
                    ports.put("vitruv-server.port", Integer.parseInt(vitruvPortStr));
                }

                // Load HTTPS server port
                String httpsPortStr = properties.getProperty("https-server.port");
                if (httpsPortStr != null) {
                    ports.put("https-server.port", Integer.parseInt(httpsPortStr));
                }
            }
        } catch (Exception e) {
            logger.error("Could not read " + DEFAULT_CONFIG_PROPERTIES_NAME + ". Error message: {}", e.getMessage(), e);
        }

        return ports;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // TODO
    private static InternalUserInteractor getInternalUserInteractor() {
        return new InternalUserInteractor() {
            @Override
            public NotificationInteractionBuilder getNotificationDialogBuilder() {
                logger.warn("getNotificationDialogBuilder() is not implemented.");
                return null;
            }

            @Override
            public ConfirmationInteractionBuilder getConfirmationDialogBuilder() {
                logger.warn("getConfirmationDialogBuilder() is not implemented.");
                return null;
            }

            @Override
            public TextInputInteractionBuilder getTextInputDialogBuilder() {
                logger.warn("getTextInputDialogBuilder() is not implemented.");
                return null;
            }

            @Override
            public MultipleChoiceSingleSelectionInteractionBuilder getSingleSelectionDialogBuilder() {
                logger.warn("getSingleSelectionDialogBuilder() is not implemented.");
                return null;
            }

            @Override
            public MultipleChoiceMultiSelectionInteractionBuilder getMultiSelectionDialogBuilder() {
                logger.warn("getMultiSelectionDialogBuilder() is not implemented.");
                return null;
            }

            @Override
            public void registerUserInputListener(UserInteractionListener userInteractionListener) {
                logger.warn("registerUserInputListener() is not implemented.");
            }

            @Override
            public void deregisterUserInputListener(UserInteractionListener userInteractionListener) {
                logger.warn("deregisterUserInputListener() is not implemented.");
            }

            @Override
            public AutoCloseable replaceUserInteractionResultProvider(Functions.Function1<? super InteractionResultProvider, ? extends InteractionResultProvider> function1) {
                logger.warn("replaceUserInteractionResultProvider() is not implemented.");
                return null;
            }
        };
    }
}
