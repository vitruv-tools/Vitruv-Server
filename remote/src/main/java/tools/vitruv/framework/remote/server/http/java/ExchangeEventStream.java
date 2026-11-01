package tools.vitruv.framework.remote.server.http.java;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import tools.vitruv.framework.remote.server.http.EventStream;

/** An {@link EventStream} writing to the response body of an {@link HttpExchange}. */
final class ExchangeEventStream implements EventStream {
  private final HttpExchange exchange;
  private final OutputStream body;
  private boolean closed;

  ExchangeEventStream(HttpExchange exchange) {
    this.exchange = exchange;
    this.body = exchange.getResponseBody();
  }

  @Override
  public synchronized void send(String event, String id, String data) throws IOException {
    StringBuilder message = new StringBuilder();
    message.append("event: ").append(event).append('\n');
    if (id != null) {
      message.append("id: ").append(id).append('\n');
    }
    for (String line : data.split("\\R", -1)) {
      message.append("data: ").append(line).append('\n');
    }
    write(message.append('\n').toString());
  }

  @Override
  public synchronized void comment(String comment) throws IOException {
    write(": " + comment + "\n\n");
  }

  private void write(String text) throws IOException {
    if (closed) {
      throw new IOException("The event stream is closed");
    }
    body.write(text.getBytes(StandardCharsets.UTF_8));
    body.flush();
  }

  @Override
  public synchronized void close() {
    if (!closed) {
      closed = true;
      exchange.close();
    }
  }
}
