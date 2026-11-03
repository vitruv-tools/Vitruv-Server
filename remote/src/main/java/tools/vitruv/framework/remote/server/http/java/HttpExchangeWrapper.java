package tools.vitruv.framework.remote.server.http.java;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import tools.vitruv.framework.remote.common.rest.constants.ContentType;
import tools.vitruv.framework.remote.common.rest.constants.Header;
import tools.vitruv.framework.remote.server.http.EventStream;
import tools.vitruv.framework.remote.server.http.HttpWrapper;

/** This is an implementation of the {@link HttpWrapper} for the Java built-in HTTP server. */
class HttpExchangeWrapper implements HttpWrapper {
  private final HttpExchange exchange;
  private int statusCode = HttpURLConnection.HTTP_OK;
  private boolean eventStream;

  /**
   * Creates a new {@link HttpExchangeWrapper}.
   *
   * @param exchange The {@link HttpExchange} to wrap.
   */
  public HttpExchangeWrapper(HttpExchange exchange) {
    this.exchange = exchange;
  }

  @Override
  public String getRequestPath() {
    return exchange.getRequestURI().getRawPath();
  }

  @Override
  public String getRequestQuery() {
    return exchange.getRequestURI().getRawQuery();
  }

  @Override
  public void setStatusCode(int statusCode) {
    this.statusCode = statusCode;
  }

  @Override
  public int getStatusCode() {
    return statusCode;
  }

  @Override
  public void addResponseHeader(String header, String value) {
    exchange.getResponseHeaders().add(header, value);
  }

  @Override
  public void setContentType(String type) {
    exchange.getResponseHeaders().set(Header.CONTENT_TYPE, type);
  }

  @Override
  public String getRequestHeader(String header) {
    return exchange.getRequestHeaders().getFirst(header);
  }

  @Override
  public String getRequestBodyAsString() throws IOException {
    return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
  }

  @Override
  public EventStream openEventStream() throws IOException {
    exchange.getResponseHeaders().set(Header.CONTENT_TYPE, ContentType.TEXT_EVENT_STREAM);
    exchange.getResponseHeaders().set("Cache-Control", "no-cache");
    // length 0: the body is sent in chunks until the stream is closed
    exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
    eventStream = true;
    return new ExchangeEventStream(exchange);
  }

  @Override
  public boolean isEventStream() {
    return eventStream;
  }

  @Override
  public void sendResponse(int responseCode) throws IOException {
    exchange.sendResponseHeaders(responseCode, -1);
  }

  @Override
  public void sendResponse(int responseCode, byte[] body) throws IOException {
    exchange.sendResponseHeaders(responseCode, body.length);
    var outputStream = exchange.getResponseBody();
    outputStream.write(body);
    outputStream.flush();
    outputStream.close();
  }
}
