package tools.vitruv.framework.remote.client.http;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;

/**
 * The default {@link VitruvHttpClientWrapper} implementation, which uses the Java {@link HttpClient}
 * as underlying client.
 */
public class JavaHttpClientWrapper implements VitruvHttpClientWrapper {
    private final HttpClient client;

    public JavaHttpClientWrapper() {
        this.client = HttpClient.newHttpClient();
    }

    @Override
    public VitruvHttpResponseWrapper sendRequest(VitruvHttpRequest request) throws Exception {
        var requestBuilder = HttpRequest
            .newBuilder()
            .uri(URI.create(request.getUri()))
            .method(
                request.getMethod(),
                request.getBody() == null ? BodyPublishers.noBody() : BodyPublishers.ofString(request.getBody())
            );

        for (var headerEntry : request.getHeaders().entrySet()) {
            requestBuilder.header(headerEntry.getKey(), headerEntry.getValue());
        }

        var response = client.send(requestBuilder.build(), BodyHandlers.ofString());
        return new JavaHttpResponseWrapper(response);
    }

    @Override
    public void disconnect() throws Exception {
    }
}
