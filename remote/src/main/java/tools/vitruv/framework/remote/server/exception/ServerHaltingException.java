package tools.vitruv.framework.remote.server.exception;

/**
 * Represents an exception which should be thrown when the processing of a REST-request is halted
 * due to an error. An HTTP-status code must be provided, since this exception is caught by the
 * {@link tools.vitruv.framework.remote.server.VitruvServer Server} in order to create error
 * response messages.
 */
public class ServerHaltingException extends RuntimeException {

  private final int statusCode;
  private final String jsonBody;

  /**
   * Creates a new {@link ServerHaltingException}.
   *
   * @param statusCode the HTTP status code representing the error
   * @param msg the error message
   */
  public ServerHaltingException(int statusCode, String msg) {
    this(statusCode, msg, null);
  }

  /**
   * Creates a new {@link ServerHaltingException} whose response body is the given JSON instead of
   * the message.
   *
   * @param statusCode the HTTP status code representing the error
   * @param msg the error message
   * @param jsonBody the JSON body of the error response, {@code null} to send the message as text
   */
  public ServerHaltingException(int statusCode, String msg, String jsonBody) {
    super(msg);
    this.statusCode = statusCode;
    this.jsonBody = jsonBody;
  }

  /**
   * Returns the JSON body of the error response.
   *
   * @return the JSON body, {@code null} if the message is sent as text
   */
  public String getJsonBody() {
    return jsonBody;
  }

  /**
   * Returns the HTTP status code representing the error.
   *
   * @return the HTTP status code
   */
  public int getStatusCode() {
    return statusCode;
  }
}
