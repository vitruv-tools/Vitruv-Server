package tools.vitruv.remote.secclient;

import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.transport.HttpClientConnectionFactory;
import org.eclipse.jetty.client.transport.HttpClientTransportDynamic;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.client.transport.ClientConnectionFactoryOverHTTP2;
import org.eclipse.jetty.http3.client.HTTP3Client;
import org.eclipse.jetty.http3.client.transport.ClientConnectionFactoryOverHTTP3;
import org.eclipse.jetty.io.ClientConnectionFactory;
import org.eclipse.jetty.io.ClientConnector;
import org.eclipse.jetty.quic.client.ClientQuicConfiguration;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import tools.vitruv.remote.seccommon.SecurityProviderInitialization;
import tools.vitruv.remote.seccommon.TlsContextConfiguration;
import tools.vitruv.remote.seccommon.cert.CertificateGenerator;

/**
 * This factory allows creating Eclipse Jetty HTTP clients for HTTPS connections.
 */
public final class JettySecureHttpClientFactory {
    static {
        SecurityProviderInitialization.initializeSecurityProviders();
    }

    private JettySecureHttpClientFactory() {}

    /**
     * Creates a new Eclipse Jetty HTTP client. It supports HTTP/1.1, HTTP/2, and HTTP/3, each secure only.
     * Due to Jetty's behavior, HTTP/3 can only be used when it is explicit set for HTTP requests.
     * 
     * @param config the configuration for the TLS layer.
     * @return the configured and created {@link HttpClient}.
     */
    public static HttpClient createSecureHttpClient(TlsContextConfiguration config) throws Exception {
        SslContextFactory.Client clientTlsContext = new SslContextFactory.Client();
        clientTlsContext.setProvider(BouncyCastleJsseProvider.PROVIDER_NAME);
        clientTlsContext.setEndpointIdentificationAlgorithm(null);

        if (config.keyStorePath() != null) {
            clientTlsContext.setKeyStorePath(config.keyStorePath().toString());
        } else if (config.keyStore() != null) {
            clientTlsContext.setKeyStore(config.keyStore());
        } else {
            clientTlsContext.setKeyStore(CertificateGenerator.createEmptyKeyStore(config.keyStorePassword()));
        }
        if (config.trustStorePath() != null) {
            clientTlsContext.setTrustStorePath(config.trustStorePath().toString());
        } else if (config.trustStore() != null) {
            clientTlsContext.setTrustStore(config.trustStore());
        } else {
            clientTlsContext.setTrustStore(CertificateGenerator.createEmptyKeyStore(config.trustStorePassword()));
        }

        clientTlsContext.setKeyStorePassword(config.keyStorePassword());
        clientTlsContext.setTrustStorePassword(config.trustStorePassword());
        
        ClientConnector client = new ClientConnector();
        client.setSslContextFactory(clientTlsContext);
        ClientConnectionFactory.Info http1 = HttpClientConnectionFactory.HTTP11;

        HTTP2Client http2Client = new HTTP2Client(client);
        ClientConnectionFactoryOverHTTP2.HTTP2 http2 = new ClientConnectionFactoryOverHTTP2.HTTP2(http2Client);

        ClientQuicConfiguration quicConfiguration = new ClientQuicConfiguration(clientTlsContext, config.tempCertDir());
        HTTP3Client http3Client = new HTTP3Client(quicConfiguration);
        ClientConnectionFactoryOverHTTP3.HTTP3 http3 = new ClientConnectionFactoryOverHTTP3.HTTP3(http3Client);

        return new HttpClient(new HttpClientTransportDynamic(client, http2, http1, http3));
    }
}
