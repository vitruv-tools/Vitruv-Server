package tools.vitruv.framework.remote.client.http;

import java.net.http.HttpResponse;
import java.util.Optional;

public class JavaHttpResponseWrapper implements VitruvHttpResponseWrapper {
    private HttpResponse<String> wrapperResponse;

    JavaHttpResponseWrapper(HttpResponse<String> response) {
        this.wrapperResponse = response;
    }

    @Override
    public int getStatusCode() {
        return this.wrapperResponse.statusCode();
    }

    @Override
    public Optional<String> getBody() {
        return Optional.ofNullable(this.wrapperResponse.body());
    }

    @Override
    public Optional<String> getHeader(String key) {
        return this.wrapperResponse.headers().firstValue(key);
    }
}
