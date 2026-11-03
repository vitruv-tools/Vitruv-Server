package tools.vitruv.framework.remote.client.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import tools.vitruv.framework.remote.client.Subscription;
import tools.vitruv.framework.remote.common.json.JsonMapper;
import tools.vitruv.framework.remote.common.rest.constraints.ConstraintStatusDto;

/**
 * Reads the Server-Sent Events of {@code /vsum/constraints/events} on an own thread and passes the
 * results of constraint validations to a listener.
 */
final class ConstraintEventSubscription implements Subscription {
  private static final String VALIDATION_EVENT = "validation";

  private final InputStream body;
  private final JsonMapper mapper;
  private final Consumer<ConstraintStatusDto> listener;
  private volatile boolean open = true;

  private ConstraintEventSubscription(
      InputStream body, JsonMapper mapper, Consumer<ConstraintStatusDto> listener) {
    this.body = body;
    this.mapper = mapper;
    this.listener = listener;
  }

  /**
   * Starts reading the events of the given response body.
   *
   * @param body the body of the event stream response
   * @param mapper the JSON mapper to read the events
   * @param listener receives the status of the constraints after every validation
   * @return the subscription
   */
  static ConstraintEventSubscription start(
      InputStream body, JsonMapper mapper, Consumer<ConstraintStatusDto> listener) {
    ConstraintEventSubscription subscription =
        new ConstraintEventSubscription(body, mapper, listener);
    Thread reader = new Thread(subscription::read, "vitruv-constraint-event-reader");
    reader.setDaemon(true);
    reader.start();
    return subscription;
  }

  private void read() {
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(body, StandardCharsets.UTF_8))) {
      String event = null;
      StringBuilder data = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.isEmpty()) {
          dispatch(event, data.toString());
          event = null;
          data.setLength(0);
        } else if (line.startsWith("event:")) {
          event = line.substring("event:".length()).strip();
        } else if (line.startsWith("data:")) {
          if (!data.isEmpty()) {
            data.append('\n');
          }
          data.append(line.substring("data:".length()).stripLeading());
        }
        // comments (": ...") and ids are ignored
      }
    } catch (IOException e) {
      // the subscription was closed or the server ended the connection
    } finally {
      open = false;
    }
  }

  private void dispatch(String event, String data) {
    if (!VALIDATION_EVENT.equals(event) || data.isEmpty()) {
      return;
    }
    try {
      listener.accept(mapper.deserialize(data, ConstraintStatusDto.class));
    } catch (IOException | RuntimeException e) {
      // a broken event or a failing listener must not end the subscription
    }
  }

  @Override
  public boolean isOpen() {
    return open;
  }

  @Override
  public void close() {
    open = false;
    try {
      body.close();
    } catch (IOException e) {
      // already closed
    }
  }
}
