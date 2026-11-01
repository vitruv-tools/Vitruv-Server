package tools.vitruv.framework.remote.server.rest.endpoints;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;
import org.eclipse.emfcloud.jackson.resource.JsonResource;
import tools.vitruv.change.atomic.uuid.Uuid;
import tools.vitruv.framework.remote.common.util.ResourceUtil;
import tools.vitruv.framework.vsum.models.ModelReadAccess;

/**
 * Copies models of a virtual model, so they can be sent without referencing live model elements.
 * Every copied element is identified by the UUID of its original in the virtual model.
 *
 * <p>References to elements of models that are not copied point to placeholders: empty elements of
 * the same type in a resource with the URI of the original model, identified by the UUID of the
 * original element. They are serialized like references between files, e.g. {@code
 * other.ecore#<uuid>}. The placeholders belong to their own resource set, so they are not
 * serialized as models.
 *
 * <p>Must be used within {@link tools.vitruv.framework.vsum.VirtualModel#readModels}.
 */
final class ModelCopier {

  /** The format of the copies. */
  enum Format {
    /** {@link JsonResource}s, identified by their {@code _id}. */
    JSON {
      @Override
      ResourceSet createResourceSet() {
        return ResourceUtil.createJsonResourceSet();
      }

      @Override
      Resource createResource(URI uri, ResourceSet resourceSet) {
        return ResourceUtil.createResourceWith(uri, List.of(), resourceSet);
      }

      @Override
      void setId(Resource resource, EObject element, String id) {
        ((JsonResource) resource).setID(element, id);
      }
    },
    /** XMI resources, identified by their {@code xmi:id}. */
    XMI {
      @Override
      ResourceSet createResourceSet() {
        return new ResourceSetImpl();
      }

      @Override
      Resource createResource(URI uri, ResourceSet resourceSet) {
        Resource resource = new XMIResourceImpl(uri);
        resourceSet.getResources().add(resource);
        return resource;
      }

      @Override
      void setId(Resource resource, EObject element, String id) {
        ((XMLResource) resource).setID(element, id);
      }
    };

    abstract ResourceSet createResourceSet();

    abstract Resource createResource(URI uri, ResourceSet resourceSet);

    abstract void setId(Resource resource, EObject element, String id);
  }

  private ModelCopier() {}

  /**
   * Copies the models with the given URIs, in the order of the models of the virtual model.
   *
   * @param access the read access to the models
   * @param uris the URIs of the models to copy, all models if {@code null}
   * @param format the format of the copies
   * @return a resource set containing one resource per copied model
   */
  static ResourceSet copy(ModelReadAccess access, Set<URI> uris, Format format) {
    ResourceSet copies = format.createResourceSet();
    // a single copier, so references between the copied models point to the copies
    EcoreUtil.Copier copier = new EcoreUtil.Copier();
    for (Resource resource : access.getModelResources()) {
      if (uris == null || uris.contains(resource.getURI())) {
        Collection<EObject> contents = copier.copyAll(resource.getContents());
        format.createResource(resource.getURI(), copies).getContents().addAll(contents);
      }
    }
    copier.copyReferences();

    Set<EObject> copiedElements = Collections.newSetFromMap(new IdentityHashMap<>());
    copiedElements.addAll(copier.values());
    Placeholders placeholders = new Placeholders(access, format);
    for (Map.Entry<EObject, EObject> entry : copier.entrySet()) {
      EObject copy = entry.getValue();
      Optional<Uuid> uuid = access.getUuid(entry.getKey());
      if (uuid.isPresent()) {
        format.setId(copy.eResource(), copy, uuid.get().getRawValue());
      }
      replaceExternalReferences(copy, copiedElements, placeholders);
    }
    return copies;
  }

  @SuppressWarnings("unchecked")
  private static void replaceExternalReferences(
      EObject copy, Set<EObject> copiedElements, Placeholders placeholders) {
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
            targets.set(i, placeholders.placeholderFor(target));
          }
        }
      } else if (value instanceof EObject target && isExternal(target, copiedElements)) {
        copy.eSet(reference, placeholders.placeholderFor(target));
      }
    }
  }

  private static boolean isExternal(EObject target, Set<EObject> copiedElements) {
    return !target.eIsProxy() && !copiedElements.contains(target);
  }

  /** Placeholders for referenced elements of models that are not copied. */
  private static final class Placeholders {
    private final ModelReadAccess access;
    private final Format format;
    private final ResourceSet resourceSet;
    private final Map<EObject, EObject> placeholders = new IdentityHashMap<>();

    Placeholders(ModelReadAccess access, Format format) {
      this.access = access;
      this.format = format;
      this.resourceSet = format.createResourceSet();
    }

    EObject placeholderFor(EObject original) {
      return placeholders.computeIfAbsent(original, this::createPlaceholder);
    }

    private EObject createPlaceholder(EObject original) {
      EObject placeholder = EcoreUtil.create(original.eClass());
      Resource originalResource = original.eResource();
      // the original URI, so references are written relative to the referencing model
      URI uri = originalResource != null ? originalResource.getURI() : URI.createURI("unknown");
      Resource resource = resourceSet.getResource(uri, false);
      if (resource == null) {
        resource = format.createResource(uri, resourceSet);
      }
      resource.getContents().add(placeholder);
      String id =
          access
              .getUuid(original)
              .map(Uuid::getRawValue)
              .orElseGet(() -> EcoreUtil.getURI(original).fragment());
      format.setId(resource, placeholder, id);
      return placeholder;
    }
  }
}
