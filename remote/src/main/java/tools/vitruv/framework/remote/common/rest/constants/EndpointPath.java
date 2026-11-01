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

  /** The endpoint path for subscribing to the results of constraint validations. */
  public static final String CONSTRAINTS_EVENTS = "/vsum/constraints/events";

  /** The endpoint path for the status of the constraints of the virtual model. */
  public static final String CONSTRAINTS_STATUS = "/vsum/constraints/status";

  /** The endpoint path for the models of the virtual model. */
  public static final String MODELS = "/vsum/models";

  /** The endpoint path for getting the content of several models at once. */
  public static final String MODELS_BATCH = "/vsum/models/batch";

  /** The endpoint path for the metamodels of the virtual model. */
  public static final String METAMODELS = "/vsum/metamodels";

  /**
   * Gets the endpoint path of a single model of the virtual model.
   *
   * @param id the id of the model
   * @return the path {@code /vsum/models/{id}} with the URL-encoded id
   */
  public static String model(String id) {
    return MODELS + "/" + encode(id);
  }

  /**
   * Gets the endpoint path of a single metamodel of the virtual model.
   *
   * @param nsUri the namespace URI of the metamodel
   * @return the path {@code /vsum/metamodels/{nsUri}} with the URL-encoded namespace URI
   */
  public static String metamodel(String nsUri) {
    return METAMODELS + "/" + encode(nsUri);
  }

  private static String encode(String segment) {
    return URLEncoder.encode(segment, StandardCharsets.UTF_8).replace("+", "%20");
  }

  /**
   * Gets the endpoint path of a single constraint of the virtual model.
   *
   * @param id the id of the constraint
   * @return the path {@code /vsum/constraints/{id}} with the URL-encoded id
   */
  public static String constraint(String id) {
    return CONSTRAINTS + "/" + encode(id);
  }

  private EndpointPath() throws InstantiationException {
    throw new InstantiationException("Cannot be instantiated");
  }
}
