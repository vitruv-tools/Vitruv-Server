package server;

import app.VitruvSecurityServerApp;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import handler.AuthEndpointHandler;
import handler.CallbackEndpointHandler;
import handler.VitruvRequestHandler;
import handler.TokenValidationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.TLSUtils;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class SecurityServerManager {
    private static final Logger logger = LoggerFactory.getLogger(SecurityServerManager.class);
    private final int port;
    private final int forwardPort;
    private final char[] tlsPassword;
    private HttpsServer server;

    public SecurityServerManager(int port, int forwardPort, String tlsPassword) {
        this.port = port;
        this.forwardPort = forwardPort;
        this.tlsPassword = tlsPassword == null ? null : tlsPassword.toCharArray();
    }

    public void start() throws Exception {
        final SSLContext sslContext = createSSLContext();

        server = HttpsServer.create(new InetSocketAddress(port), 0);
        server.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
            @Override
            public void configure(HttpsParameters params) {
                configureHttpsParameters(params, getSSLContext());
            }
        });

        //// Endpoints ////
        // Vitruv endpoints (secured through TokenValidationHandler wrapper)
        server.createContext("/", new TokenValidationHandler(new VitruvRequestHandler(forwardPort)));

        // VitruvServer endpoints
        server.createContext("/auth", new AuthEndpointHandler());
        server.createContext("/callback", new CallbackEndpointHandler());

        server.setExecutor(null);
        server.start();

        logger.info("HTTPS server started on port " + port);
    }

    private void configureHttpsParameters(HttpsParameters params, SSLContext sslContext) {
        SSLEngine engine = sslContext.createSSLEngine();
        params.setNeedClientAuth(false);
        params.setCipherSuites(engine.getEnabledCipherSuites());
        params.setProtocols(engine.getEnabledProtocols());
        params.setSSLParameters(sslContext.getDefaultSSLParameters());
    }

    private SSLContext createSSLContext() throws Exception {
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            X509Certificate certificate;

            // load certificate
            try (InputStream certChainStream = new FileInputStream(VitruvSecurityServerApp.getServerConfig().getCertChainPath())) {
                logger.debug("certChainStream: {}", certChainStream);
                certificate = (X509Certificate) certificateFactory.generateCertificate(certChainStream);
            }

            // load private key
            try (InputStream keyStream = new FileInputStream(VitruvSecurityServerApp.getServerConfig().getCertKeyPath())) {
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
            throw new Exception("Failed to initialize SSL context", e);
        }
    }
}
