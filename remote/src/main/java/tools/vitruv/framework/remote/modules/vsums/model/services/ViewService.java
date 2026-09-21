package tools.vitruv.framework.remote.modules.vsums.model.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import tools.vitruv.framework.remote.common.utils.ResourceUtils;
import tools.vitruv.framework.remote.common.utils.json.JsonFieldName;
import tools.vitruv.framework.remote.modules.vsums.async.InteractionContext;
import tools.vitruv.framework.remote.modules.vsums.model.entities.ViewUpdate;
import tools.vitruv.framework.remote.modules.vsums.model.entities.ViewUpdateRepo;
import tools.vitruv.framework.remote.modules.vsums.model.entities.VsumInfo;
import tools.vitruv.framework.remote.modules.vsums.model.entities.VsumInfoRepo;
import tools.vitruv.framework.remote.modules.vsums.model.wrapper.SelectorWrapper;
import tools.vitruv.framework.remote.modules.vsums.model.wrapper.ViewWrapper;
import tools.vitruv.framework.remote.modules.vsums.model.wrapper.VsumWrapper;
import edu.kit.ipd.sdq.commons.util.org.eclipse.emf.ecore.resource.ResourceCopier;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emfcloud.jackson.resource.JsonResource;
import org.springframework.stereotype.Service;
import tools.vitruv.change.atomic.hid.HierarchicalId;
import tools.vitruv.change.atomic.root.InsertRootEObject;
import tools.vitruv.change.composite.description.VitruviusChange;
import tools.vitruv.change.composite.description.VitruviusChangeFactory;
import tools.vitruv.framework.views.View;
import tools.vitruv.framework.views.changederivation.DefaultStateBasedChangeResolutionStrategy;
import tools.vitruv.framework.views.changederivation.StateBasedChangeResolutionStrategy;
import tools.vitruv.framework.views.impl.ModifiableView;
import tools.vitruv.framework.views.impl.ViewCreatingViewType;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("resource")
@Service
@RequiredArgsConstructor
@Slf4j
public class ViewService {
    private final ViewUpdateRepo viewUpdateRepo;
    private final VsumInfoRepo vsumInfoRepo;

    private final StateBasedChangeResolutionStrategy resolutionStrategy =
            new DefaultStateBasedChangeResolutionStrategy();

    /**
     * Loads the last update for a given VsumWrapper instance and applies it to the view.
     *
     * @param vsumWrapper the VsumWrapper instance for which the last update is to be loaded.
     */
    public void loadLastUpdate(@NonNull VsumWrapper vsumWrapper) {
        val viewUpdate = viewUpdateRepo.findFirstByVsumInfoIdOrderByTimestampDesc(vsumWrapper.info().getId());
        if (viewUpdate == null) {
            return;
        }

        val selectorWrapper = createSelector(vsumWrapper, viewUpdate.getViewTypeName());
        val selectedObjects = selectorWrapper.selectableObjects().values().stream()
                .filter(it -> viewUpdate.getSelectedObjectEClassNames().contains(it.eClass().getInstanceTypeName()))
                .toArray(EObject[]::new);

        val viewWrapper = openView(selectorWrapper, selectedObjects);

        InteractionContext.runInStateReplay(() -> {
            try (val view = viewWrapper.view()) {
                commitResourceSet(viewWrapper, viewUpdate.getEncodedResourceSet());
                view.update();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public record ViewInfo(boolean isClosed, Boolean isOutdated) {
    }

    /**
     * Retrieves information about the state of a view wrapped in a ViewWrapper instance.
     * This method determines if the view is closed and, if applicable, whether it is outdated.
     * Additionally, if the view is closed but present, a specified action will be executed.
     *
     * @param viewWrapper              the wrapper object containing the view to be examined; may be null
     * @param onViewIsClosedButPresent a Runnable to be executed if the view is closed but present
     * @return a ViewInfo object containing the closed state and outdated state (if applicable) of the view
     */
    public ViewInfo getViewInfo(ViewWrapper viewWrapper, @NonNull Runnable onViewIsClosedButPresent) {
        if (viewWrapper == null) {
            return new ViewInfo(true, null);
        }

        val isClosed = viewWrapper.view().isClosed();

        if (isClosed) {
            onViewIsClosedButPresent.run();
        }

        val isOutdated = isClosed ? null : viewWrapper.view().isOutdated();

        return new ViewInfo(isClosed, isOutdated);
    }

    /**
     * Creates a new selector for a specified view type in the virtual model.
     *
     * @param vsumWrapper  a wrapper containing the virtual model and related metadata; must not be null
     * @param viewTypeName the name of the view type to be used for creating the selector; must not be null
     * @return a {@link SelectorWrapper} containing the created selector, its associated metadata,
     * and the mapping between UUIDs and elements
     * @throws IllegalArgumentException if the specified view type name is not found in the virtual model
     */
    public SelectorWrapper createSelector(@NonNull VsumWrapper vsumWrapper, @NonNull String viewTypeName) {
        val viewType = vsumWrapper.virtualModel().getViewTypes().stream()
                .filter(it -> it.getName().equals(viewTypeName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("View type not found: " + viewTypeName));

        val selectorId = UUID.randomUUID();

        val selector = vsumWrapper.virtualModel().createSelector(viewType);
        val originalSelection = selector.getSelectableElements().stream().toList();
        val copiedSelection = EcoreUtil.copyAll(originalSelection).stream().toList();

        // Wrap selection in resource for serialization.
        val resource = (JsonResource) ResourceUtils.createResourceWith(
                URI.createURI(JsonFieldName.TEMP_VALUE), copiedSelection
        );

        // Create EObject to UUID mapping.
        val objects = new HashMap<UUID, EObject>();
        for (int i = 0; i < originalSelection.size(); i++) {
            val objectId = UUID.randomUUID();
            objects.put(objectId, originalSelection.get(i));
            resource.setID(copiedSelection.get(i), objectId.toString());
        }

        return new SelectorWrapper(selectorId, selector, objects, resource, vsumWrapper);
    }

    /**
     * Opens a view by processing the selected objects and creating a new view with the given selector.
     *
     * @param selectorWrapper The wrapper containing the selector used to manage object selection and view creation.
     * @param selectedObjects An array of EObjects that will be selected and included in the view.
     * @return A {@code ViewWrapper} containing details of the created view, including its ID, the view instance,
     * a copied ResourceSet, and the associated vsumWrapper.
     */
    public ViewWrapper openView(@NonNull SelectorWrapper selectorWrapper, @NonNull EObject[] selectedObjects) {
        for (EObject selectedObject : selectedObjects) {
            selectorWrapper.selector().setSelected(selectedObject, true);
        }

        UUID viewId = UUID.randomUUID();
        View view = selectorWrapper.selector().createView();

        List<Resource> resources =
                view.getRootObjects().stream().map(EObject::eResource).distinct().toList();
        ResourceSet resourceSet = new ResourceSetImpl();
        ResourceCopier.copyViewResources(resources, resourceSet);

        return new ViewWrapper(viewId, view, resourceSet, selectorWrapper.vsumWrapper());
    }

    /**
     * Commits changes to the resource set associated with a given view wrapper. It processes
     * the provided serialized resource set, identifies changes, and applies those changes
     * to the current resource set, if any exist. The method returns a change object
     * representing all the committed changes.
     *
     * @param viewWrapper        The wrapper around the view and its related utilities. Must not be null.
     * @param encodedResourceSet The serialized representation of the resource set to commit. Must not be null.
     * @return A {@code VitruviusChange} object representing the committed changes, or {@code null}
     * if no changes were detected.
     * @throws RuntimeException If the serialized resource set cannot be deserialized, or if
     *                          the changes are rejected during commitment.
     */
    @SuppressWarnings("unchecked")
    public VitruviusChange<?> commitResourceSet(@NonNull ViewWrapper viewWrapper, @NonNull String encodedResourceSet) {

        final ResourceSet resourceSet;
        try {
            resourceSet = viewWrapper.vsumWrapper().jsonMapper().deserialize(encodedResourceSet, ResourceSet.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize request body: {}", e.getMessage());
            throw new RuntimeException(e.getMessage());
        }

        val currentResources =
                viewWrapper.view().getRootObjects().stream().map(EObject::eResource).distinct().toList();
        val resourceMap = new HashMap<URI, Resource>();
        for (Resource resource : currentResources) {
            resourceMap.put(resource.getURI(), resource);
        }

        val allChanges = new LinkedList<VitruviusChange<HierarchicalId>>();
        resourceSet
                .getResources()
                .forEach(it -> {
                    val resource = resolveExistingResource(resourceMap, currentResources, it.getURI());
                    if (resource == null) {
                        throw new RuntimeException(
                                "Cannot match committed resource to open view: " + it.getURI());
                    }
                    val changes = findChanges(resource, it);
                    if (!changes.getEChanges().isEmpty()) {
                        allChanges.add(changes);
                    }
                });

        if (allChanges.isEmpty()) {
            log.info("No changes detected.");
            return null;
        }

        @SuppressWarnings("rawtypes")
        VitruviusChange change = VitruviusChangeFactory.getInstance().createCompositeChange(allChanges);

        change
                .getEChanges()
                .forEach(it -> {
                    if (it instanceof InsertRootEObject<?> echange) {
                        echange.setResource(new ResourceImpl(URI.createURI(echange.getUri())));
                    }
                });

        val type = (ViewCreatingViewType<?, ?>) viewWrapper.view().getViewType();
        try {
            type.commitViewChanges((ModifiableView) viewWrapper.view(), change);
        } catch (RuntimeException e) {
            throw new RuntimeException("Changes rejected: " + e.getMessage());
        }

        return change;
    }

    /**
     * Serializes the current open view resource set without running {@code view.update()}.
     */
    public String serializeResourceSet(@NonNull ViewWrapper viewWrapper) {
        val resources = viewWrapper.view().getRootObjects().stream().map(EObject::eResource).distinct().toList();
        val resourceSet = new ResourceSetImpl();
        ResourceCopier.copyViewResources(resources, resourceSet);

        try {
            return viewWrapper.vsumWrapper().jsonMapper().serialize(resourceSet);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Persists the current view state as a {@link ViewUpdate} without applying propagation.
     * Used when parking an inconsistency so the hub can visualize in-progress models.
     */
    public void saveViewSnapshot(@NonNull ViewWrapper viewWrapper) {
        if (viewWrapper.view().isClosed()) {
            return;
        }

        String encodedResourceSet = serializeResourceSet(viewWrapper);

        val selectedObjectNames = viewWrapper.view().getRootObjects().stream()
                .map(rootObject -> rootObject.eClass().getInstanceTypeName())
                .toList();

        UUID vsumId = viewWrapper.vsumWrapper().info().getId();
        VsumInfo managedInfo = vsumInfoRepo.findById(vsumId).orElse(null);
        if (managedInfo == null) {
            log.warn(
                    "Skipping view-update snapshot for missing VSUM {} (row deleted while a view/task was still open).",
                    vsumId);
            return;
        }

        val viewUpdate = new ViewUpdate(
                managedInfo,
                viewWrapper.view().getViewType().getName(),
                selectedObjectNames,
                encodedResourceSet,
                Instant.now()
        );
        viewUpdateRepo.save(viewUpdate);
    }

    /**
     * Applies the update of the given {@link ViewWrapper} and persists the changes in a repository.
     * The method processes the view's resources, serializes the resource set to a string,
     * and constructs a {@link ViewUpdate} object for saving in the repository.
     *
     * @param viewWrapper the wrapper encapsulating the view and related metadata;
     *                    must not be null
     * @return a serialized string representation of the updated resource set
     * @throws RuntimeException if an issue occurs during resource serialization
     */
    public String update(@NonNull ViewWrapper viewWrapper) {
        viewWrapper.view().update();

        String encodedResourceSet = serializeResourceSet(viewWrapper);

        val selectedObjectNames = viewWrapper.view().getRootObjects().stream()
                .map(rootObject -> rootObject.eClass().getInstanceTypeName())
                .toList();

        UUID vsumId = viewWrapper.vsumWrapper().info().getId();
        VsumInfo managedInfo = vsumInfoRepo.findById(vsumId).orElse(null);
        if (managedInfo == null) {
            log.warn(
                    "Skipping view-update snapshot for missing VSUM {} (row deleted while a view/task was still open).",
                    vsumId);
            return encodedResourceSet;
        }

        val viewUpdate = new ViewUpdate(
                managedInfo,
                viewWrapper.view().getViewType().getName(),
                selectedObjectNames,
                encodedResourceSet,
                Instant.now()
        );
        viewUpdateRepo.save(viewUpdate);

        return encodedResourceSet;
    }

    private VitruviusChange<HierarchicalId> findChanges(Resource oldState, Resource newState) {
        if (oldState == null) {
            return resolutionStrategy.getChangeSequenceForCreated(newState);
        } else if (newState == null) {
            return resolutionStrategy.getChangeSequenceForDeleted(oldState);
        } else {
            return resolutionStrategy.getChangeSequenceBetween(newState, oldState);
        }
    }

    /**
     * Match incoming commit resources to open-view resources even when URI prefixes differ
     * (e.g. client sends {@code /example.model2}, view holds an absolute vsum-storage file URI).
     * Added for SystemRootVsum after {@code persistProjectRelative} / {@code IdTransformation} fixes;
     * see SystemRootVsumProvider/README.md.
     */
    private Resource resolveExistingResource(
            Map<URI, Resource> resourceMap, List<Resource> currentResources, URI incomingUri) {
        val directMatch = resourceMap.get(incomingUri);
        if (directMatch != null) {
            return directMatch;
        }

        val incomingKey = uriMatchKey(incomingUri);
        for (Resource resource : currentResources) {
            if (uriMatchKey(resource.getURI()).equals(incomingKey)) {
                return resource;
            }
        }
        return null;
    }

    private static String uriMatchKey(URI uri) {
        if (uri == null) {
            return "";
        }
        return uri.lastSegment();
    }
}
