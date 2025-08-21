package tools.vitruv.framework.remote.client.jetty;

import org.eclipse.jetty.client.CompletableResponseListener;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.Request;
import org.eclipse.jetty.client.StringRequestContent;
import org.eclipse.jetty.http.HttpVersion;

import tools.vitruv.framework.remote.client.http.VitruvHttpClientWrapper;
import tools.vitruv.framework.remote.client.http.VitruvHttpRequest;
import tools.vitruv.framework.remote.client.http.VitruvHttpResponseWrapper;
import tools.vitruv.framework.remote.common.AvailableHttpVersions;

/**
 * A wrapper for the Eclipse Jetty HTTP client. It supports clear-text HTTP/1.1 and clear-text HTTP/2.
 * Before the wrapper can be used, it needs to be initialized.
 */
public class JettyHttpClientWrapper implements VitruvHttpClientWrapper {
    private AvailableHttpVersions fixedVersion;
    private HttpClient client;

    /**
     * Initializes the wrapped HTTP client by creating and starting it.
     * 
     * @throws Exception if the creation fails.
     */
    public void initialize() throws Exception {
        this.client = this.createHttpClient();
        this.client.start();
    }

    /**
     * Sets the HTTP version to use for future requests. This wrapper supports clear-text HTTP/1.1 and HTTP/2.
     * 
     * @param version the specific HTTP version to use.
     */
    public void setFixedVersion(AvailableHttpVersions version) {
        this.fixedVersion = version;
    }

    protected AvailableHttpVersions getFixedHttpVersion() {
        return this.fixedVersion;
    }

    protected HttpClient createHttpClient() throws Exception {
        return JettyHttpClientFactory.createClearTextHttpClient();
    }

    @Override
    public VitruvHttpResponseWrapper sendRequest(VitruvHttpRequest request) throws Exception {
        var actualRequest = this.prepareActualRequest(request);

        var response = new CompletableResponseListener(actualRequest, 100 * 1024 * 1024).send().get();

        return new JettyHttpResponseWrapper(response);
    }

    protected Request prepareActualRequest(VitruvHttpRequest request) {
        var actualRequest = this.client
            .newRequest(request.getUri())
            .method(request.getMethod())
            .headers((mutable) -> {
                for (var headerEntry : request.getHeaders().entrySet()) {
                    mutable.add(headerEntry.getKey(), headerEntry.getValue());
                }
            });

        if (this.fixedVersion == AvailableHttpVersions.HTTP_1_1) {
            actualRequest.version(HttpVersion.HTTP_1_1);
        } else if (this.fixedVersion == AvailableHttpVersions.HTTP_2) {
            actualRequest.version(HttpVersion.HTTP_2);
        }
        
        if (request.getBody() != null) {
            actualRequest.body(new StringRequestContent(request.getBody()));
        }

        return actualRequest;
    }

    @Override
    public void disconnect() throws Exception {
        this.client.stop();
    }
}
