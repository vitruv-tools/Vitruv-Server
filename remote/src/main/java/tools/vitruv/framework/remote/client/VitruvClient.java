package tools.vitruv.framework.remote.client;

import java.util.List;
import tools.vitruv.framework.remote.client.exception.BadServerResponseException;
import tools.vitruv.framework.remote.common.rest.constraints.ConstraintDto;
import tools.vitruv.framework.remote.common.rest.constraints.ConstraintValidationRequest;
import tools.vitruv.framework.remote.common.rest.constraints.ConstraintValidationResponse;
import tools.vitruv.framework.views.ViewProvider;
import tools.vitruv.framework.views.ViewTypeProvider;

/**
 * A Vitruvius client can remotely access the available {@link
 * tools.vitruv.framework.views.ViewType}s of a Vitruvius instance and query {@link
 * tools.vitruv.framework.views.ViewSelector}s in order to create remotely editable {@link
 * tools.vitruv.framework.views.View}s. It can also list and validate the constraints of the
 * Vitruvius instance.
 */
public interface VitruvClient extends ViewTypeProvider, ViewProvider {

  /**
   * Returns the constraints registered at the virtual model of the Vitruvius instance.
   *
   * @return the constraints
   */
  List<ConstraintDto> getConstraints();

  /**
   * Validates constraints of the virtual model of the Vitruvius instance.
   *
   * <p>If the affected models are requested, they are stored in the temporary folder of the client
   * like the models of views, at their path relative to the project root of the server.
   *
   * @param request the constraints to validate and whether the affected models are returned
   * @return the validation results
   * @throws BadServerResponseException if a constraint does not exist or the request is invalid
   */
  ConstraintValidationResponse validateConstraints(ConstraintValidationRequest request);
}
