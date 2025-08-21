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

    /**
     * Sets the general configuration for the client. This must be set before initializing the client.
     * 
     * @param config the configuration.
     */
    public void setConfiguration(TlsContextConfiguration config) {
        this.config = config;
    }

    /**
     * The {@link JettyVitruvServer} gives session IDs out. After obtaining one, it can be set with this method
     * to use it for future requests.
     * 
     * @param sessionId the session ID to use.
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    protected HttpClient createHttpClient() throws Exception {
        return JettySecureHttpClientFactory.createSecureHttpClient(this.config);
    }

    @Override
    protected Request prepareActualRequest(VitruvHttpRequest request) {
        var actualRequest = super.prepareActualRequest(request);

        if (this.getFixedHttpVersion() == AvailableHttpVersions.HTTP_3) {
            actualRequest.version(HttpVersion.HTTP_3);
        }

        if (this.sessionId != null) {
            actualRequest.cookie(HttpCookie.from(SessionConstants.SESSION_COOKIE_NAME, this.sessionId));
        }

        return actualRequest;
    }
}
