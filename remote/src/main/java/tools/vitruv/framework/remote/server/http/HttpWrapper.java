package tools.vitruv.framework.remote.server.http;

import java.io.IOException;

/** This interface wraps an HTTP request/response from the underlying HTTP server implementation. */
public interface HttpWrapper {
  /**
   * Returns the path of the request URI, still URL-encoded.
   *
   * @return The raw request path, e.g. {@code /vsum/constraints/a%3A%3Ab}.
   */
  String getRequestPath();

  /**
   * Sets the status code sent when the endpoint returns normally. The default is 200 OK.
   *
   * @param statusCode The status code of the response.
   */
  void setStatusCode(int statusCode);

  /**
   * Returns the status code sent when the endpoint returns normally.
   *
   * @return The status code of the response.
   */
  int getStatusCode();

  /**
   * Returns a response header.
   *
   * @param header Name of the header.
   * @return The value of the header.
   */
  String getRequestHeader(String header);

  /**
   * Returns the request body converted to a String.
   *
   * @return The request body as String.
   * @throws IOException If the body cannot be read or if the conversion fails.
   */
  String getRequestBodyAsString() throws IOException;

  /**
   * Adds a value to the response header.
   *
   * @param header Name of the header.
   * @param value The value of the header to add.
   */
  void addResponseHeader(String header, String value);

  /**
   * Sets the content type of the response.
   *
   * @param type The content type.
   */
  void setContentType(String type);

  /**
   * Sends an HTTP response without a body.
   *
   * @param responseCode The status code of the response.
   * @throws IOException If the response cannot be sent.
   */
  void sendResponse(int responseCode) throws IOException;

  /**
   * Sends an HTTP response with a body.
   *
   * @param responseCode The status code of the response.
   * @param body The body of the response.
   * @throws IOException If the response cannot be sent.
   */
  void sendResponse(int responseCode, byte[] body) throws IOException;
}
