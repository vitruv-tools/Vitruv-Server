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

  private Header() throws InstantiationException {
    throw new InstantiationException("Cannot be instantiated");
  }
}
