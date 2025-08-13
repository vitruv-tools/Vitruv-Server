package tools.vitruv.framework.remote.client.http;

/**
 * This interface provides a unified way for the {@link VitruvRemoteConnection} to access
 * an HTTP client for the underlying HTTP communication with a Vitruvius server.
 * Configuration of the wrapped HTTP client is implementation-specific.
 */
public interface VitruvHttpClientWrapper {
    /**
     * Sends a request with the HTTP client, wrapped by a concrete implementation.
     * 
     * @param request The information for the HTTP request.
     * @return The wrapped response.
     * @throws Exception If there is an issue with sending the HTTP request.
     */
    VitruvHttpResponseWrapper sendRequest(VitruvHttpRequest request) throws Exception;

    /**
     * Disconnects the underlying HTTP client from the server, and cleans up allocated resources.
     * 
     * @throws Exception If the client cannot be disconnected.
     */
    void disconnect() throws Exception;
}
