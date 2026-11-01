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
   * Returns the query of the request URI, still URL-encoded.
   *
   * @return The raw query without {@code ?}, e.g. {@code id=a%3A%3Ab&id=c}, or {@code null} if the
   *     request has none.
   */
  default String getRequestQuery() {
    return null;
  }

  /**
   * Starts a response that stays open and sends Server-Sent Events. The endpoint must return
   * {@code null} afterwards; the response is sent through the returned stream until it is closed.
   *
   * @return the open event stream
   * @throws IOException if the response cannot be started
   * @throws UnsupportedOperationException if this server does not support event streams
   */
  default EventStream openEventStream() throws IOException {
    throw new UnsupportedOperationException("Event streams are not supported");
  }

  /**
   * Checks whether the response was turned into an event stream by {@link #openEventStream()}.
   *
   * @return {@code true} if the response is an open event stream
   */
  default boolean isEventStream() {
    return false;
  }

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
