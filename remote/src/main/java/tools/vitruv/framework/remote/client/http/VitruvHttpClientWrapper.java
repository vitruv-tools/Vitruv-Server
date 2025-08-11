package tools.vitruv.framework.remote.client.http;

public interface VitruvHttpClientWrapper {
    VitruvHttpResponseWrapper sendRequest(VitruvHttpRequest request) throws Exception;
}
