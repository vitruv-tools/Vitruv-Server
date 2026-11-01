package tools.vitruv.framework.remote.common.rest.constants;

/** A class holding constants for content types used in REST communication. */
public final class ContentType {
  /** The content type for JSON data. */
  public static final String APPLICATION_JSON = "application/json";

  /** The content type for plain text data. */
  public static final String TEXT_PLAIN = "text/plain";

  /** The content type for Server-Sent Events. */
  public static final String TEXT_EVENT_STREAM = "text/event-stream";

  /** The content type for models and metamodels serialized as XMI. */
  public static final String APPLICATION_XMI = "application/xmi+xml";

  private ContentType() throws InstantiationException {
    throw new InstantiationException("Cannot be instantiated");
  }
}
