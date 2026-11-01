package tools.vitruv.framework.remote.client;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.eclipse.emf.ecore.resource.ResourceSet;
import tools.vitruv.framework.remote.client.exception.BadServerResponseException;
import tools.vitruv.framework.remote.common.rest.constraints.ConstraintDto;
import tools.vitruv.framework.remote.common.rest.constraints.ConstraintStatusDto;
import tools.vitruv.framework.remote.common.rest.constraints.ConstraintValidationRequest;
import tools.vitruv.framework.remote.common.rest.constraints.ConstraintValidationResponse;
import tools.vitruv.framework.remote.common.rest.models.MetamodelDto;
import tools.vitruv.framework.remote.common.rest.models.ModelDto;
import tools.vitruv.framework.views.ViewProvider;
import tools.vitruv.framework.views.ViewTypeProvider;

/**
 * A Vitruvius client can remotely access the available {@link
 * tools.vitruv.framework.views.ViewType}s of a Vitruvius instance and query {@link
 * tools.vitruv.framework.views.ViewSelector}s in order to create remotely editable {@link
 * tools.vitruv.framework.views.View}s. It can also list and validate the constraints and read the
 * models and metamodels of the Vitruvius instance.
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

  /**
   * Validates constraints of the virtual model of the Vitruvius instance and returns only their
   * status, without violations or models.
   *
   * @param constraintIds the constraints to validate, {@code null} for all constraints
   * @return the status per constraint and the revision of the constraints
   * @throws BadServerResponseException if a constraint does not exist or an id is empty
   */
  ConstraintStatusDto getConstraintStatus(Collection<String> constraintIds);

  /**
   * Subscribes to the results of constraint validations of the virtual model. The listener first
   * receives the current status of all constraints, then the status after every propagated change
   * and every change of a constraint. It is called on an own thread of the subscription.
   *
   * @param listener receives the status of the constraints
   * @return the subscription, to be closed when no more events are needed
   * @throws BadServerResponseException if the server does not provide constraint events
   */
  Subscription subscribeConstraintEvents(Consumer<ConstraintStatusDto> listener);

  /**
   * Lists the models of the virtual model of the Vitruvius instance.
   *
   * @return the models with their ids and metamodels, sorted by id
   */
  List<ModelDto> getModelDescriptors();

  /**
   * Gets the content of models of the virtual model, all taken from the same state. Like the models
   * of views, they are stored in the temporary folder of the client; every element is identified by
   * its UUID in the virtual model, which {@link org.eclipse.emf.ecore.resource.Resource#getEObject}
   * resolves.
   *
   * @param ids the ids of the models, see {@link ModelDto#id()}; {@code null} for all models
   * @return the models
   * @throws BadServerResponseException if a model does not exist
   */
  ResourceSet getModelContents(Collection<String> ids);

  /**
   * Gets the content of a model of the virtual model as XMI, in which every element has its UUID in
   * the virtual model as {@code xmi:id}.
   *
   * @param id the id of the model, see {@link ModelDto#id()}
   * @return the XMI document
   * @throws BadServerResponseException if the model does not exist
   */
  String getModelXmi(String id);

  /**
   * Lists the metamodels of the models of the virtual model of the Vitruvius instance.
   *
   * @return the metamodels
   */
  List<MetamodelDto> getMetamodelDescriptors();

  /**
   * Gets a metamodel of the virtual model as {@code .ecore} XMI.
   *
   * @param nsUri the namespace URI of the metamodel
   * @return the XMI document
   * @throws BadServerResponseException if the virtual model has no such metamodel
   */
  String getMetamodelXmi(String nsUri);
}
