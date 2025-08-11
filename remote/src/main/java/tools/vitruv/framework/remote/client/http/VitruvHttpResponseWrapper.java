package tools.vitruv.framework.remote.client.http;

import java.util.Optional;

public interface VitruvHttpResponseWrapper {
    int getStatusCode();
    Optional<String> getBody();
    Optional<String> getHeader(String key);
}
