package tools.vitruv.remote.secclient;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.Request;
import org.eclipse.jetty.http.HttpCookie;
import org.eclipse.jetty.http.HttpVersion;

import tools.vitruv.framework.remote.client.http.VitruvHttpRequest;
import tools.vitruv.framework.remote.client.jetty.JettyHttpClientWrapper;
import tools.vitruv.framework.remote.common.AvailableHttpVersions;
import tools.vitruv.remote.seccommon.SecurityProviderInitialization;
import tools.vitruv.remote.seccommon.SessionConstants;
import tools.vitruv.remote.seccommon.TlsContextConfiguration;

/**
 * A wrapper for the Eclipse Jetty HTTP client. It supports HTTP/1.1, HTTP/2, and HTTP/3 (experimental), only secured.
 */
public class SecurityJettyHttpClientWrapper extends JettyHttpClientWrapper {
    static {
        SecurityProviderInitialization.initializeSecurityProviders();
    }
    
    private TlsContextConfiguration config;
    private String sessionId;
    private AvailableHttpVersions fixedVersion;

    /**
     * Sets the general configuration for the client. This must be set before initializing the client.
     * 
     * @param config the configuration.
     */
    public void setConfiguration(TlsContextConfiguration config) {
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
    protected HttpClient createHttpClient() throws Exception {
        return JettySecureHttpClientFactory.createSecureHttpClient(this.config);
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
