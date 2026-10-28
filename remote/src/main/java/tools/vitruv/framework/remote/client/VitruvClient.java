package tools.vitruv.framework.remote.client;

import java.util.List;
import java.util.Optional;
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
   * Returns the constraint with the given id.
   *
   * @param id the id of the constraint
   * @return the constraint, empty if there is none
   */
  Optional<ConstraintDto> getConstraint(String id);

  /**
   * Creates a constraint at the virtual model of the Vitruvius instance. It is validated by the
   * next validation and still exists after a restart of the server.
   *
   * @param language the language of the constraint, e.g. {@code vitruvocl}
   * @param source the source of exactly one constraint
   * @return the created constraint
   * @throws BadServerResponseException with status 400 if the source is invalid (the body lists the
   *     diagnostics), 409 if the id already exists or 422 if the language is not supported
   */
  ConstraintDto createConstraint(String language, String source);

  /**
   * Replaces the source of a constraint, including constraints the virtual model was built with.
   *
   * @param id the id of the constraint
   * @param source the new source, which must define a constraint with the same id
   * @return the updated constraint
   * @throws BadServerResponseException with status 400 if the source is invalid, 404 if there is
   *     no constraint with the id or 409 if the constraint cannot be changed
   */
  ConstraintDto updateConstraint(String id, String source);

  /**
   * Deletes a constraint, including constraints the virtual model was built with.
   *
   * @param id the id of the constraint
   * @throws BadServerResponseException with status 404 if there is no constraint with the id or
   *     409 if the constraint cannot be changed
   */
  void deleteConstraint(String id);

  /**
   * Returns the revision of the constraints, which changes whenever a constraint is created,
   * updated or deleted.
   *
   * @return the revision
   */
  String getConstraintRevision();

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
