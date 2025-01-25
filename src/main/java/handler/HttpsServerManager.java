package handler;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.HttpsRequestHandler;

import javax.net.ssl.*;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;

public class HttpsServerManager {
    private static final Logger logger = LoggerFactory.getLogger(HttpsServerManager.class);
    /**
     * Default name of the keystore file containing the self-signed certificate.
     */
    private static final String DEFAULT_KEYSTORE_NAME = "keystore.p12";
    /**
     * Default password for the keystore containing the self-signed certificate.
     */
    private static final String DEFAULT_KEYSTORE_PASSWORD = "password";
    private final int port;
    private final int forwardPort;
    private HttpsServer server;

    public HttpsServerManager(int port, int forwardPort) {
        this.port = port;
        this.forwardPort = forwardPort;
    }

    public void start() throws Exception {
        final SSLContext sslContext = createSSLContext(DEFAULT_KEYSTORE_NAME, DEFAULT_KEYSTORE_PASSWORD);

        server = HttpsServer.create(new InetSocketAddress(port), 0);
        server.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
            @Override
            public void configure(HttpsParameters params) {
                SSLContext c = getSSLContext();
                SSLEngine engine = c.createSSLEngine();
                params.setNeedClientAuth(false);
                params.setCipherSuites(engine.getEnabledCipherSuites());
                params.setProtocols(engine.getEnabledProtocols());
                params.setSSLParameters(c.getDefaultSSLParameters());
            }
        });

        server.createContext("/", new HttpsRequestHandler(forwardPort));
        server.setExecutor(null);
        server.start();
        logger.info("HTTPS server started on port " + port);

    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            logger.info("HTTPS Server stopped.");
        }
    }

    private SSLContext createSSLContext(String keystoreFile, String password) throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(keystoreFile)) {
            if (is == null) {
                logger.error("Keystore not found: " + keystoreFile);
                throw new Exception("Keystore not found: " + keystoreFile);
            }
            ks.load(is, password.toCharArray());
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, password.toCharArray());

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), null, null);
        return sslContext;
    }
}