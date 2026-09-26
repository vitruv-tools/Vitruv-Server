package tools.vitruv.framework.remote.modules.vsums.usecases;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.vitruv.framework.remote.modules.vsums.model.entities.OpenInconsistency;
import tools.vitruv.framework.remote.modules.vsums.model.entities.OpenInconsistencyRepo;
import tools.vitruv.framework.remote.modules.vsums.model.entities.OpenInconsistencyState;
import tools.vitruv.framework.remote.modules.vsums.model.entities.VsumInfoRepo;
import tools.vitruv.framework.remote.modules.vsums.model.manager.VsumManager;
import tools.vitruv.framework.remote.modules.vsums.model.services.ViewService;
import tools.vitruv.framework.remote.modules.vsums.model.wrapper.SelectorWrapper;
import tools.vitruv.framework.remote.modules.vsums.model.wrapper.VsumWrapper;
import tools.vitruv.framework.remote.modules.vsums.usecases.dtos.InconsistencyContextResponse;
import tools.vitruv.framework.remote.modules.vsums.usecases.dtos.InconsistencyContextResponse.CorrespondenceEdge;
import tools.vitruv.framework.remote.modules.vsums.usecases.dtos.InconsistencyContextResponse.ModelElementNode;

import java.util.*;

/**
 * Builds a metamodel-agnostic element + correspondence snapshot for Hub visualization.
 * Works for SystemRoot and AmaltheaAscet by reading the VSUM's view selector and correspondence model.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InconsistencyVisualizationUseCases {

  private static final int MAX_ELEMENTS = 250;

  private final OpenInconsistencyRepo openInconsistencyRepo;
  private final VsumInfoRepo vsumInfoRepo;
  private final VsumManager vsumManager;
  private final ViewService viewService;

  public InconsistencyContextResponse getContext(UUID inconsistencyId) {
    OpenInconsistency entity = openInconsistencyRepo.findById(inconsistencyId).orElseThrow(() ->
        new ResponseStatusException(HttpStatus.NOT_FOUND, "Inconsistency not found: " + inconsistencyId));

    // Live VSUM access can deadlock while propagation is parked on the same virtual model.
    if (entity.getState() == OpenInconsistencyState.OPEN) {
      String metamodelName = vsumInfoRepo.findById(entity.getVsumId())
          .map(info -> info.getMetaModelName())
          .orElse(null);
      return emptyContext(
          entity,
          metamodelName,
          "Correspondences are unavailable while propagation is paused at this inconsistency. "
              + "The model diagram still shows the parked snapshot.");
    }

    VsumWrapper vsumWrapper;
    try {
      vsumWrapper = vsumManager.getVsum(entity.getVsumId());
    } catch (Exception e) {
      log.warn("Could not load VSUM {} for inconsistency {}: {}", entity.getVsumId(), inconsistencyId, e.toString());
      return emptyContext(entity, null, "VSUM could not be loaded for visualization: " + safeMessage(e));
    }

    String viewTypeName = vsumWrapper.virtualModel().getViewTypes().stream()
        .map(vt -> vt.getName())
        .findFirst()
        .orElse(null);
    if (viewTypeName == null) {
      return emptyContext(entity, vsumWrapper.info().getMetaModelName(), "VSUM has no view types");
    }

    SelectorWrapper selectorWrapper;
    try {
      selectorWrapper = viewService.createSelector(vsumWrapper, viewTypeName);
    } catch (Exception e) {
      log.warn("Could not create selector for VSUM {}: {}", entity.getVsumId(), e.toString());
      return emptyContext(
          entity,
          vsumWrapper.info().getMetaModelName(),
          "Could not open view selector for visualization: " + safeMessage(e));
    }

    Object correspondenceModel = null;
    try {
      Object virtualModel = vsumWrapper.virtualModel();
      if (virtualModel instanceof tools.vitruv.framework.vsum.internal.InternalVirtualModel internal) {
        correspondenceModel = internal.getCorrespondenceModel();
      } else {
        var method = virtualModel.getClass().getMethod("getCorrespondenceModel");
        correspondenceModel = method.invoke(virtualModel);
      }
    } catch (Exception e) {
      log.debug("No correspondence model for VSUM {}: {}", entity.getVsumId(), e.toString());
    }

    Map<EObject, String> ids = new IdentityHashMap<>();
    Map<String, ModelElementNode> elements = new LinkedHashMap<>();
    Set<String> edgeKeys = new LinkedHashSet<>();
    List<CorrespondenceEdge> edges = new ArrayList<>();

    for (EObject root : selectorWrapper.selectableObjects().values()) {
      collectTree(root, null, ids, elements, correspondenceModel, edges, edgeKeys);
      if (elements.size() >= MAX_ELEMENTS) {
        break;
      }
    }

    String note = elements.isEmpty()
        ? "No selectable model elements found for view type '" + viewTypeName + "'."
        : "Showing up to " + MAX_ELEMENTS + " elements from view type '" + viewTypeName
        + "' with Vitruvius correspondences.";

    return new InconsistencyContextResponse(
        entity.getId(),
        entity.getVsumId(),
        entity.getVsumName(),
        vsumWrapper.info().getMetaModelName(),
        viewTypeName,
        note,
        List.copyOf(elements.values()),
        List.copyOf(edges)
    );
  }

  private void collectTree(
      EObject object,
      String parentId,
      Map<EObject, String> ids,
      Map<String, ModelElementNode> elements,
      Object correspondenceModel,
      List<CorrespondenceEdge> edges,
      Set<String> edgeKeys
  ) {
    if (object == null || elements.size() >= MAX_ELEMENTS) {
      return;
    }
    if (ids.containsKey(object)) {
      return;
    }

    String id = UUID.randomUUID().toString();
    ids.put(object, id);
    String label = displayName(object);
    String metamodel = metamodelName(object);
    elements.put(id, new ModelElementNode(
        id,
        label,
        object.eClass().getName(),
        metamodel,
        safeUri(object),
        roleFor(metamodel),
        parentId
    ));

    for (EObject other : correspondingObjects(correspondenceModel, object)) {
      collectTree(other, null, ids, elements, correspondenceModel, edges, edgeKeys);
      String otherId = ids.get(other);
      if (otherId == null) {
        continue;
      }
      String key = id.compareTo(otherId) < 0 ? id + "|" + otherId : otherId + "|" + id;
      if (edgeKeys.add(key)) {
        edges.add(new CorrespondenceEdge(id, otherId, label, displayName(other)));
      }
    }

    for (EObject child : object.eContents()) {
      collectTree(child, id, ids, elements, correspondenceModel, edges, edgeKeys);
      if (elements.size() >= MAX_ELEMENTS) {
        return;
      }
    }
  }

  private Iterable<EObject> correspondingObjects(Object correspondenceModel, EObject object) {
    if (correspondenceModel == null) {
      return List.of();
    }
    try {
      var method = correspondenceModel.getClass().getMethod("getCorrespondingEObjects", EObject.class);
      Object result = method.invoke(correspondenceModel, object);
      if (result instanceof Iterable<?> iterable) {
        List<EObject> objects = new ArrayList<>();
        for (Object item : iterable) {
          if (item instanceof EObject eObject) {
            objects.add(eObject);
          }
        }
        return objects;
      }
    } catch (ReflectiveOperationException e) {
      log.debug("Could not read correspondences: {}", e.toString());
    }
    return List.of();
  }

  private InconsistencyContextResponse emptyContext(OpenInconsistency entity, String metamodel, String note) {
    return new InconsistencyContextResponse(
        entity.getId(),
        entity.getVsumId(),
        entity.getVsumName(),
        metamodel,
        null,
        note,
        List.of(),
        List.of()
    );
  }

  private static String displayName(EObject object) {
    EStructuralFeature nameFeature = object.eClass().getEStructuralFeature("name");
    if (nameFeature instanceof EAttribute && object.eIsSet(nameFeature)) {
      Object value = object.eGet(nameFeature);
      if (value != null && !String.valueOf(value).isBlank()) {
        return object.eClass().getName() + " " + value;
      }
    }
    return object.eClass().getName();
  }

  private static String metamodelName(EObject object) {
    if (object.eClass().getEPackage() == null) {
      return "unknown";
    }
    String name = object.eClass().getEPackage().getName();
    return name != null ? name : object.eClass().getEPackage().getNsPrefix();
  }

  private static String roleFor(String metamodel) {
    String normalized = metamodel == null ? "" : metamodel.toLowerCase(Locale.ROOT);
    if (normalized.contains("amalthea") || normalized.contains("system") || normalized.equals("model")) {
      return "source";
    }
    if (normalized.contains("ascet") || normalized.contains("root") || normalized.contains("model2")) {
      return "target";
    }
    return "context";
  }

  private static String safeUri(EObject object) {
    try {
      return EcoreUtil.getURI(object).toString();
    } catch (RuntimeException e) {
      return object.eClass().getName();
    }
  }

  private static String safeMessage(Exception e) {
    return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
  }
}
