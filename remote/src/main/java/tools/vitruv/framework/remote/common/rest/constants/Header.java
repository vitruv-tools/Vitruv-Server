package tools.vitruv.framework.remote.common.rest.constants;

/** Constants for HTTP headers used in the Vitruv Remote Framework. */
public final class Header {
  /** The Content-Type header key. */
  public static final String CONTENT_TYPE = "Content-Type";

  /** The View-UUID header key. */
  public static final String VIEW_UUID = "View-UUID";

  /** The Selector-UUID header key. */
  public static final String SELECTOR_UUID = "Selector-UUID";

  /** The View-Type header key. */
  public static final String VIEW_TYPE = "View-Type";

  /** The ETag header key, carrying the revision of the constraints. */
  public static final String ETAG = "ETag";

  /** The If-Match header key, guarding changes of constraints against a revision. */
  public static final String IF_MATCH = "If-Match";

  /** The Location header key, carrying the path of a created constraint. */
  public static final String LOCATION = "Location";

  /** The Accept header key, selecting the format of models (JSON or XMI). */
  public static final String ACCEPT = "Accept";

  private Header() throws InstantiationException {
    throw new InstantiationException("Cannot be instantiated");
  }
}
