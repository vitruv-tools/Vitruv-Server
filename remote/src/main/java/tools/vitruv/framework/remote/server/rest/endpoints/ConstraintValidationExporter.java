package tools.vitruv.framework.remote.server.rest.endpoints;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emfcloud.jackson.resource.JsonResource;
import tools.vitruv.change.atomic.uuid.Uuid;
import tools.vitruv.change.atomic.uuid.UuidResolver;
import tools.vitruv.framework.remote.common.json.IdTransformation;
import tools.vitruv.framework.remote.common.rest.constraints.ConstraintResultDto;
import tools.vitruv.framework.remote.common.rest.constraints.ConstraintValidationResponse;
import tools.vitruv.framework.remote.common.rest.constraints.ViolationDto;
import tools.vitruv.framework.remote.common.util.ResourceUtil;
import tools.vitruv.framework.views.ViewSource;
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
   * @throws IllegalStateException if models are requested but the virtual model does not provide
   *     access to them
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
        violation.uuid() != null ? Uuids.toRawValue(violation.uuid()) : null,
        packageName != null ? packageName + "::" + eClass.getName() : eClass.getName(),
        violation.resourceUri() != null ? toLocal(violation.resourceUri()) : null,
        violation.severity().name(),
        violation.message());
  }

  private String toLocal(URI uri) {
    return transformation.toLocal(uri).toString();
  }

  private ResourceSet copyAffectedModels(Set<URI> affectedUris) {
    if (!(model instanceof ViewSource source)) {
      throw new IllegalStateException("The virtual model does not provide access to its models");
    }
    UuidResolver uuidResolver = source.getUuidResolver();
    ResourceSet copies = ResourceUtil.createJsonResourceSet();
    // a single copier, so references between the affected models point to the copies
    EcoreUtil.Copier copier = new EcoreUtil.Copier();
    List<JsonResource> copiedResources = new ArrayList<>();
    for (Resource resource : source.getViewSourceModels()) {
      if (affectedUris.contains(resource.getURI())) {
        Collection<EObject> contents = copier.copyAll(resource.getContents());
        copiedResources.add(
            (JsonResource) ResourceUtil.createResourceWith(resource.getURI(), contents, copies));
      }
    }
    copier.copyReferences();

    Set<EObject> copiedElements = Collections.newSetFromMap(new IdentityHashMap<>());
    copiedElements.addAll(copier.values());
    ExternalTargets externalTargets = new ExternalTargets(uuidResolver);
    for (Map.Entry<EObject, EObject> entry : copier.entrySet()) {
      EObject original = entry.getKey();
      EObject copy = entry.getValue();
      if (uuidResolver.hasUuid(original)) {
        JsonResource resource = (JsonResource) copy.eResource();
        resource.setID(copy, Uuids.toRawValue(uuidResolver.getUuid(original)));
      }
      replaceExternalReferences(copy, copiedElements, externalTargets);
    }
    return copies;
  }

  /**
   * Replaces references of the given copy to elements outside the copied models by placeholders,
   * so that the copies do not reference live elements of the virtual model.
   */
  @SuppressWarnings("unchecked")
  private void replaceExternalReferences(
      EObject copy, Set<EObject> copiedElements, ExternalTargets externalTargets) {
    for (EReference reference : copy.eClass().getEAllReferences()) {
      if (reference.isContainment()
          || reference.isContainer()
          || reference.isDerived()
          || reference.isTransient()
          || !reference.isChangeable()
          || !copy.eIsSet(reference)) {
        continue;
      }
      Object value = copy.eGet(reference, false);
      if (reference.isMany()) {
        EList<EObject> targets = (EList<EObject>) value;
        for (int i = 0; i < targets.size(); i++) {
          EObject target = targets.get(i);
          if (isExternal(target, copiedElements)) {
            targets.set(i, externalTargets.placeholderFor(target));
          }
        }
      } else if (value instanceof EObject target && isExternal(target, copiedElements)) {
        copy.eSet(reference, externalTargets.placeholderFor(target));
      }
    }
  }

  private static boolean isExternal(EObject target, Set<EObject> copiedElements) {
    return !target.eIsProxy() && !copiedElements.contains(target);
  }

  /**
   * Placeholders for referenced elements of models that are not copied. A placeholder is an empty
   * element of the same type in a resource with the URI of the original model, identified by the
   * UUID of the original element, so references to it are serialized as {@code <model URI
   * relative to the referencing model>#<UUID>}, like references between XMI files. The
   * placeholders belong to their own resource set, so they are not serialized as models.
   */
  private final class ExternalTargets {
    private final UuidResolver uuidResolver;
    private final ResourceSet resourceSet = ResourceUtil.createJsonResourceSet();
    private final Map<EObject, EObject> placeholders = new IdentityHashMap<>();

    ExternalTargets(UuidResolver uuidResolver) {
      this.uuidResolver = uuidResolver;
    }

    EObject placeholderFor(EObject original) {
      return placeholders.computeIfAbsent(original, this::createPlaceholder);
    }

    private EObject createPlaceholder(EObject original) {
      EObject placeholder = EcoreUtil.create(original.eClass());
      Resource originalResource = original.eResource();
      // the original URI, so references are written relative to the referencing model
      URI uri =
          originalResource != null ? originalResource.getURI() : URI.createURI("unknown");
      Resource resource = resourceSet.getResource(uri, false);
      if (resource == null) {
        resource = resourceSet.createResource(uri);
      }
      resource.getContents().add(placeholder);
      String id =
          uuidResolver.hasUuid(original)
              ? Uuids.toRawValue(uuidResolver.getUuid(original))
              : EcoreUtil.getURI(original).fragment();
      ((JsonResource) resource).setID(placeholder, id);
      return placeholder;
    }
  }

  /** Access to the textual value of {@link Uuid}s. */
  static final class Uuids {
    private static final String PREFIX = "Uuid(";
    private static final String SUFFIX = ")";

    private Uuids() {}

    /**
     * Returns the raw value of the given UUID.
     *
     * <p>{@link Uuid} only exposes its raw value via {@link Uuid#toString()} ({@code Uuid(<raw>)});
     * this method should use a public accessor once Vitruv-Change provides one.
     *
     * @param uuid the UUID
     * @return the raw value
     */
    static String toRawValue(Uuid uuid) {
      String value = uuid.toString();
      if (value.startsWith(PREFIX) && value.endsWith(SUFFIX)) {
        return value.substring(PREFIX.length(), value.length() - SUFFIX.length());
      }
      return value;
    }
  }
}
