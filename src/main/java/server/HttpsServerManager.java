package server;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import handler.AuthEndpointHandler;
import handler.CallbackEndpointHandler;
import handler.HttpsRequestHandler;
import handler.TokenValidationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;

public class HttpsServerManager {
    private static final Logger logger = LoggerFactory.getLogger(HttpsServerManager.class);
    private final int port;
    private final int forwardPort;
    private HttpsServer server;

    public HttpsServerManager(int port, int forwardPort) {
        this.port = port;
        this.forwardPort = forwardPort;
    }

    public void start() throws Exception {
        final SSLContext sslContext = createSSLContext();

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

        // Vitruv endpoints (secured through TokenValidationHandler wrapper)
        server.createContext("/", new TokenValidationHandler(new HttpsRequestHandler(forwardPort)));

        // VitruvServer endpoints
        server.createContext("/auth", new AuthEndpointHandler());
        server.createContext("/callback", new CallbackEndpointHandler());

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

    public SSLContext createSSLContext() throws Exception {
        // load certificate
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        X509Certificate certificate;
        // TODO: mount certificate in container instead using 'resources'
//        try (InputStream certChainStream = new FileInputStream(VitruvServerApp.getServerConfig().getCertChainPath())) {
        //
        try (InputStream certChainStream = getClass().getClassLoader().getResourceAsStream("fullchain.pem")) {

            certificate = (X509Certificate) certificateFactory.generateCertificate(certChainStream);
        }

        // Load private key
        // TODO: mount key in container instead using 'resources'
//        try (InputStream keyStream = new FileInputStream(VitruvServerApp.getServerConfig().getCertKeyPath())) {
        try (InputStream keyStream = getClass().getClassLoader().getResourceAsStream("privkey.der")) {
            assert keyStream != null;
            byte[] keyBytes = keyStream.readAllBytes();
            PrivateKey privateKey = streamToPrivateKey(keyBytes);

            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(null, null); // TODO: use password

            // add certificate and private key to key store
            ks.setKeyEntry("alias", privateKey, null, new Certificate[] { certificate });

            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, null);

            // create new ssl context
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), null, null);
            return sslContext;
        }
    }

    public PrivateKey streamToPrivateKey(byte[] pkcs8key) throws GeneralSecurityException {
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(pkcs8key);
        KeyFactory factory = KeyFactory.getInstance("EC");
        return factory.generatePrivate(spec);
    }
}
