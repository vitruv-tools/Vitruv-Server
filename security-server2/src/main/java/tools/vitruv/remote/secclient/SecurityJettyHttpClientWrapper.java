package tools.vitruv.remote.secclient;

import java.security.KeyStore;

import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.Request;
import org.eclipse.jetty.client.transport.HttpClientConnectionFactory;
import org.eclipse.jetty.client.transport.HttpClientTransportDynamic;
import org.eclipse.jetty.http.HttpCookie;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import tools.vitruv.framework.remote.client.http.VitruvHttpRequest;
import tools.vitruv.framework.remote.client.jetty.JettyHttpClientWrapper;
import tools.vitruv.framework.remote.common.AvailableHttpVersions;
import tools.vitruv.remote.seccommon.SecurityProviderInitialization;
import tools.vitruv.remote.seccommon.SessionConstants;

import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.client.transport.ClientConnectionFactoryOverHTTP2;
import org.eclipse.jetty.http3.client.HTTP3Client;
import org.eclipse.jetty.http3.client.transport.ClientConnectionFactoryOverHTTP3;
import org.eclipse.jetty.io.ClientConnectionFactory;
import org.eclipse.jetty.io.ClientConnector;
import org.eclipse.jetty.quic.client.ClientQuicConfiguration;

/**
 * A wrapper for the Eclipse Jetty HTTP client. It supports HTTP/1.1, HTTP/2, and HTTP/3 (experimental), only secured.
 */
public class SecurityJettyHttpClientWrapper extends JettyHttpClientWrapper {
    static {
        SecurityProviderInitialization.initializeSecurityProviders();
    }
    
    private SecurityClientConfiguration config;
    private String sessionId;
    private AvailableHttpVersions fixedVersion;

    /**
     * Sets the general configuration for the client. This must be set before initializing the client.
     * 
     * @param config the configuration.
     */
    public void setConfiguration(SecurityClientConfiguration config) {
        this.config = config;
    }

    /**
     * 
     * @param sessionId
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * This wrapper supports 
     * 
     * @param version the specific HTTP version to use.
     */
    public void setFixedVersion(AvailableHttpVersions version) {
        this.fixedVersion = version;
    }

    @Override
    protected HttpClient creaHttpClient() throws Exception {
        var keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, this.config.trustStorePassword().toCharArray());

        SslContextFactory.Client clientTlsContext = new SslContextFactory.Client();
        clientTlsContext.setProvider(BouncyCastleJsseProvider.PROVIDER_NAME);
        clientTlsContext.setEndpointIdentificationAlgorithm(null);
        clientTlsContext.setKeyStore(keyStore);
        clientTlsContext.setKeyStorePassword(this.config.trustStorePassword());
        clientTlsContext.setTrustStorePassword(this.config.trustStorePassword());
        if (this.config.trustStorePassword() != null && !this.config.trustStorePath().isBlank()) {
            clientTlsContext.setTrustStorePath(this.config.trustStorePath());
        } else {
            clientTlsContext.setTrustStore(this.config.trustStore());
        }
        
        ClientConnector client = new ClientConnector();
        client.setSslContextFactory(clientTlsContext);
        ClientConnectionFactory.Info http1 = HttpClientConnectionFactory.HTTP11;

        HTTP2Client http2Client = new HTTP2Client(client);
        ClientConnectionFactoryOverHTTP2.HTTP2 http2 = new ClientConnectionFactoryOverHTTP2.HTTP2(http2Client);

        ClientQuicConfiguration quicConfiguration = new ClientQuicConfiguration(clientTlsContext, this.config.tempCertDir());
        HTTP3Client http3Client = new HTTP3Client(quicConfiguration);
        ClientConnectionFactoryOverHTTP3.HTTP3 http3 = new ClientConnectionFactoryOverHTTP3.HTTP3(http3Client);

        return new HttpClient(new HttpClientTransportDynamic(client, http2, http1, http3));
    }

    @Override
    protected Request prepareActualRequest(VitruvHttpRequest request) {
        var actualRequest = super.prepareActualRequest(request);

        if (this.fixedVersion != null) {
            switch (this.fixedVersion) {
                default:
                case HTTP_1_1:
                    actualRequest.version(HttpVersion.HTTP_1_1);
                    break;
                case HTTP_2:
                    actualRequest.version(HttpVersion.HTTP_2);
                    break;
                case HTTP_3:
                    actualRequest.version(HttpVersion.HTTP_3);
                    break;
            }
        }

        if (this.sessionId != null) {
            actualRequest.cookie(HttpCookie.from(SessionConstants.SESSION_COOKIE_NAME, this.sessionId));
        }

        return actualRequest;
    }
}
