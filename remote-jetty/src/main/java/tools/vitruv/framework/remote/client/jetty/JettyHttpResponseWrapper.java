package tools.vitruv.framework.remote.client.jetty;

import java.util.Optional;

import org.eclipse.jetty.client.ContentResponse;

import tools.vitruv.framework.remote.client.http.VitruvHttpResponseWrapper;

/**
 * A wrapper for a Jetty HTTP response.
 */
public class JettyHttpResponseWrapper implements VitruvHttpResponseWrapper {
    private ContentResponse response;

    JettyHttpResponseWrapper(ContentResponse response) {
        this.response = response;
    }

    @Override
    public int getStatusCode() {
        return this.response.getStatus();
    }

    @Override
    public Optional<String> getBody() {
        return Optional.ofNullable(this.response.getContentAsString());
    }

    @Override
    public Optional<String> getHeader(String key) {
        return Optional.ofNullable(this.response.getHeaders().get(key));
    }
}
