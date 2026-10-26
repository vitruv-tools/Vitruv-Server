package tools.vitruv.framework.remote.server.rest.endpoints;

import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;
import tools.vitruv.framework.remote.server.http.HttpWrapper;

/** An {@link HttpWrapper} that keeps request and response in memory, to test endpoints alone. */
class FakeHttpWrapper implements HttpWrapper {
  private final Map<String, String> requestHeaders = new HashMap<>();
  private final Map<String, String> responseHeaders = new HashMap<>();
  private final String requestBody;
  private String contentType;
  private int responseCode = HttpURLConnection.HTTP_OK;

  FakeHttpWrapper(String requestBody) {
    this.requestBody = requestBody;
  }

  FakeHttpWrapper withHeader(String header, String value) {
    requestHeaders.put(header, value);
    return this;
  }

  String responseHeader(String header) {
    return responseHeaders.get(header);
  }

  String contentType() {
    return contentType;
  }

  int responseCode() {
    return responseCode;
  }

  @Override
  public String getRequestHeader(String header) {
    return requestHeaders.get(header);
  }

  @Override
  public String getRequestBodyAsString() {
    return requestBody;
  }

  @Override
  public void addResponseHeader(String header, String value) {
    responseHeaders.put(header, value);
  }

  @Override
  public void setContentType(String type) {
    contentType = type;
  }

  @Override
  public void setResponseCode(int responseCode) {
    this.responseCode = responseCode;
  }

  @Override
  public void sendResponse(int responseCode) {
    throw new UnsupportedOperationException("endpoints return the body, the server sends it");
  }

  @Override
  public void sendResponse(int responseCode, byte[] body) {
    throw new UnsupportedOperationException("endpoints return the body, the server sends it");
  }
}
