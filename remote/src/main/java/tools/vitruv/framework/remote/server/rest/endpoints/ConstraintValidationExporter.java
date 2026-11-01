package tools.vitruv.framework.remote.server.rest.endpoints;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.resource.ResourceSet;
import tools.vitruv.framework.remote.common.json.IdTransformation;
import tools.vitruv.framework.remote.common.rest.constraints.ConstraintResultDto;
import tools.vitruv.framework.remote.common.rest.constraints.ConstraintValidationResponse;
import tools.vitruv.framework.remote.common.rest.constraints.ViolationDto;
import tools.vitruv.framework.remote.server.rest.endpoints.ModelCopier.Format;
import tools.vitruv.framework.vsum.VirtualModel;
import tools.vitruv.framework.vsum.constraints.ConstraintValidationReport;
import tools.vitruv.framework.vsum.constraints.ConstraintValidationResult;
import tools.vitruv.framework.vsum.constraints.ResolvedConstraintViolation;

/**
 * Validates constraints of a virtual model and converts the report into transfer objects,
 * optionally including copies of the affected models in which every element is identified by its
 * UUID.
 *
 * <p>The virtual model must synchronize validation and change propagation on itself, as {@code
 * VirtualModelImpl} does. Its monitor is held while validating, converting and copying, so the
 * result reflects a single state of the virtual model and references no live model elements.
 */
class ConstraintValidationExporter {
  private final VirtualModel model;
  private final IdTransformation transformation;

  ConstraintValidationExporter(VirtualModel model, IdTransformation transformation) {
    this.model = model;
    this.transformation = transformation;
  }

  /**
   * Validates the given constraints.
   *
   * @param constraintIds the ids of the constraints, all constraints if {@code null}; all ids must
   *     belong to constraints of the virtual model
   * @param includeModels whether copies of the affected models are returned
   * @return the response
   * @throws UnsupportedOperationException if models are requested but the virtual model does not
   *     provide its models
   */
  ConstraintValidationResponse validate(Collection<String> constraintIds, boolean includeModels) {
    synchronized (model) {
      ConstraintValidationReport report =
          constraintIds == null
              ? model.validateConstraints()
              : model.validateConstraints(constraintIds);
      List<ConstraintResultDto> results = report.results().stream().map(this::toDto).toList();
      ResourceSet models = includeModels ? copyAffectedModels(report.affectedResourceUris()) : null;
      return new ConstraintValidationResponse(report.isSatisfied(), results, models);
    }
  }

  private ConstraintResultDto toDto(ConstraintValidationResult result) {
    List<ViolationDto> violations = result.violations().stream().map(this::toDto).toList();
    return new ConstraintResultDto(
        result.descriptor().id(),
        result.descriptor().name(),
        result.status().name(),
        result.descriptor().severity().name(),
        violations,
        result.errors());
  }

  private ViolationDto toDto(ResolvedConstraintViolation violation) {
    EClass eClass = violation.element().eClass();
    String packageName = eClass.getEPackage() != null ? eClass.getEPackage().getName() : null;
    return new ViolationDto(
        violation.uuid() != null ? violation.uuid().getRawValue() : null,
        packageName != null ? packageName + "::" + eClass.getName() : eClass.getName(),
        violation.resourceUri() != null ? toLocal(violation.resourceUri()) : null,
        violation.severity().name(),
        violation.message());
  }

  private String toLocal(URI uri) {
    return transformation.toLocal(uri).toString();
  }

  private ResourceSet copyAffectedModels(Set<URI> affectedUris) {
    return model.readModels(access -> ModelCopier.copy(access, affectedUris, Format.JSON));
  }
}
