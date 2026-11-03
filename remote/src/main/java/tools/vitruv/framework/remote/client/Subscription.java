package tools.vitruv.framework.remote.client;

/** A subscription to events of a Vitruvius server, which ends when it is closed. */
public interface Subscription extends AutoCloseable {

  /**
   * Checks whether events are still received.
   *
   * @return {@code false} after closing or when the server ended the connection
   */
  boolean isOpen();

  /** Ends the subscription. */
  @Override
  void close();
}
