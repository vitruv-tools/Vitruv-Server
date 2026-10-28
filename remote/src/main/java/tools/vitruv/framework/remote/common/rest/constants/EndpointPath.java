package tools.vitruv.framework.remote.common.rest.constants;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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

  /** The endpoint path for the constraints of the virtual model. */
  public static final String CONSTRAINTS = "/vsum/constraints";

  /** The endpoint path for validating constraints of the virtual model. */
  public static final String CONSTRAINTS_VALIDATE = "/vsum/constraints/validate";

  /** The endpoint path for the revision of the constraints of the virtual model. */
  public static final String CONSTRAINTS_REVISION = "/vsum/constraints/revision";

  /**
   * Gets the endpoint path of a single constraint of the virtual model.
   *
   * @param id the id of the constraint
   * @return the path {@code /vsum/constraints/{id}} with the URL-encoded id
   */
  public static String constraint(String id) {
    return CONSTRAINTS
        + "/"
        + URLEncoder.encode(id, StandardCharsets.UTF_8).replace("+", "%20");
  }

  private EndpointPath() throws InstantiationException {
    throw new InstantiationException("Cannot be instantiated");
  }
}
