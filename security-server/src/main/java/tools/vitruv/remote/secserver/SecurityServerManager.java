package tools.vitruv.remote.secserver;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;

import tools.vitruv.remote.secserver.config.ConfigManager;
import tools.vitruv.remote.secserver.handler.AuthEndpointHandler;
import tools.vitruv.remote.secserver.handler.CallbackEndpointHandler;
import tools.vitruv.remote.secserver.handler.TokenValidationHandler;
import tools.vitruv.remote.secserver.handler.VitruvRequestHandler;
import tools.vitruv.remote.secserver.oidc.OIDCClient;
import tools.vitruv.remote.secserver.util.TLSUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 *  Configures and launches the Security Server using TLS encryption.
 *  It defines endpoints for token validation, initiating the OIDC authentication process, and handling the callback response.
 *  The class also establishes the SSL context.
 */
class SecurityServerManager {
    private static final Logger logger = LoggerFactory.getLogger(SecurityServerManager.class);
    private final int port;
    private final int forwardPort;
    private final char[] tlsPassword;
    private HttpsServer securityServer;
    private OIDCClient oidcClient;
    private ConfigManager config;

    public SecurityServerManager(int port, int forwardPort, String tlsPassword, OIDCClient client, ConfigManager config) {
        this.port = port;
        this.forwardPort = forwardPort;
        this.tlsPassword = tlsPassword == null ? null : tlsPassword.toCharArray();
        this.config = config;
        oidcClient = client;
    }

    /**
     * Initializes this manager by setting up the TLS context,
     * configuring the HTTPS server, and registering the endpoints.
     * 
     * @throws IOException if TLS or server setup fails.
     */
    public void initialize() throws IOException {
        final SSLContext sslContext = createSSLContext();

        securityServer = HttpsServer.create(new InetSocketAddress(port), 0);
        securityServer.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
            @Override
            public void configure(HttpsParameters params) {
                configureHttpsParameters(params, getSSLContext());
            }
        });
        registerEndpoints();
        securityServer.setExecutor(null);
    }

    /**
     * Starts the server.
     */
    public void start() {
        securityServer.start();

        logger.info("Security Server started on port {} with forwardPort {}.", port, forwardPort);
    }

    /**
     * Stops the server.
     */
    public void stop() {
        securityServer.stop(0);

        logger.info("Stopped security server.");
    }

    /**
     * Registers all HTTP endpoints handled by the Security Server. See handler classes for further details.
     */
    private void registerEndpoints() {
        // Vitruv endpoints (secured through TokenValidationHandler wrapper)
        securityServer.createContext("/", new TokenValidationHandler(new VitruvRequestHandler(forwardPort), oidcClient));

        // Security Server specific endpoints
        securityServer.createContext("/auth", new AuthEndpointHandler(oidcClient));
        securityServer.createContext("/callback", new CallbackEndpointHandler(oidcClient));
    }

    private void configureHttpsParameters(HttpsParameters params, SSLContext sslContext) {
        SSLEngine engine = sslContext.createSSLEngine();
        params.setNeedClientAuth(false);
        params.setCipherSuites(engine.getEnabledCipherSuites());
        params.setProtocols(engine.getEnabledProtocols());
        params.setSSLParameters(sslContext.getDefaultSSLParameters());
    }

    /**
     * Creates and initializes the SSLContext using the TLS certificate and private key.
     *
     * @return initiated SSLContext
     */
    private SSLContext createSSLContext() {
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            X509Certificate certificate;

            // load certificate
            try (InputStream certChainStream = new FileInputStream(config.getCertChainPath())) {
                logger.debug("certChainStream: {}", certChainStream);
                certificate = (X509Certificate) certificateFactory.generateCertificate(certChainStream);
            }

            // load private key
            try (InputStream keyStream = new FileInputStream(config.getCertKeyPath())) {
                logger.debug("keyStream: {}", keyStream);

                byte[] keyBytes = keyStream.readAllBytes();
                PrivateKey privateKey = TLSUtils.convertToPkcs8Key(keyBytes);

                logger.debug("Private Key Algorithm: {}", privateKey.getAlgorithm());
                logger.debug("Private Key Format: {}", privateKey.getFormat());

                KeyStore ks = KeyStore.getInstance("PKCS12");
                ks.load(null, tlsPassword);

                // add certificate and private key to key store
                ks.setKeyEntry("alias", privateKey, tlsPassword, new Certificate[]{certificate});

                KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
                kmf.init(ks, tlsPassword);

                // create new SSL (TLS) context
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(kmf.getKeyManagers(), null, null);
                return sslContext;
            }
        } catch (Exception e) {
            logger.error("Error occurred while trying to load SSL context: {}", e.getMessage());
            throw new RuntimeException("Failed to initialize SSL context", e);
        }
    }
}
