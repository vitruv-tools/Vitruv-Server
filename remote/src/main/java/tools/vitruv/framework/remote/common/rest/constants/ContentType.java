package tools.vitruv.framework.remote.common.rest.constants;

/** A class holding constants for content types used in REST communication. */
public final class ContentType {
  /** The content type for JSON data. */
  public static final String APPLICATION_JSON = "application/json";

  /** The content type for plain text data. */
  public static final String TEXT_PLAIN = "text/plain";

  private ContentType() throws InstantiationException {
    throw new InstantiationException("Cannot be instantiated");
  }
}
