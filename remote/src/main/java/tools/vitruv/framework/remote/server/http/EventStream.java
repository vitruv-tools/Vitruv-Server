package tools.vitruv.framework.remote.server.http;

import java.io.IOException;

/**
 * An open response that sends Server-Sent Events ({@code text/event-stream}) to a client until it
 * is closed. Can be used from any thread; sending is synchronized.
 */
public interface EventStream extends AutoCloseable {

  /**
   * Sends an event.
   *
   * @param event the name of the event
   * @param id the id of the event, {@code null} for none
   * @param data the data of the event; line breaks are sent as several data lines
   * @throws IOException if the client is no longer connected
   */
  void send(String event, String id, String data) throws IOException;

  /**
   * Sends a comment, which clients ignore, e.g. to keep the connection alive.
   *
   * @param comment the comment
   * @throws IOException if the client is no longer connected
   */
  void comment(String comment) throws IOException;

  /** Closes the response. */
  @Override
  void close();
}
