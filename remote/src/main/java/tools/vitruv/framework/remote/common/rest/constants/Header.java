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

  /** The Task-Id header key, identifying a change that is propagated asynchronously. */
  public static final String TASK_ID = "Task-Id";

  /** The Prefer header key (RFC 7240). */
  public static final String PREFER = "Prefer";

  /** The Preference-Applied header key (RFC 7240). */
  public static final String PREFERENCE_APPLIED = "Preference-Applied";

  /**
   * The preference that asks the server to answer before it has processed the request (RFC 7240). A
   * change sent with this value in the {@link #PREFER} header is propagated asynchronously.
   */
  public static final String RESPOND_ASYNC = "respond-async";

  private Header() throws InstantiationException {
    throw new InstantiationException("Cannot be instantiated");
  }
}
