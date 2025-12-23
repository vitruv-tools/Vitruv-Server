package tools.vitruv.framework.remote.common.rest.constants;

/** Constants for endpoint paths used in the Vitruvius remote framework. */
public final class EndpointPath {
  /** The health check endpoint path. */
  public static final String HEALTH = "/health";

  /** The endpoint path for view types. */
  public static final String VIEW_TYPES = "/vsum/view/types";

  /** The endpoint path for view selectors. */
  public static final String VIEW_SELECTOR = "/vsum/view/selector";

  /** The endpoint path for views. */
  public static final String VIEW = "/vsum/view";

  /** The endpoint path to check if a view is closed. */
  public static final String IS_VIEW_CLOSED = "/vsum/view/closed";

  /** The endpoint path to check if a view is outdated. */
  public static final String IS_VIEW_OUTDATED = "/vsum/view/outdated";

  /** The endpoint path for deriving changes. */
  public static final String CHANGE_DERIVING = "/vsum/view/derive-changes";

  private EndpointPath() throws InstantiationException {
    throw new InstantiationException("Cannot be instantiated");
  }
}
