package tools.vitruv.framework.remote.modules.vsums.usecasesimpls;

import com.fasterxml.jackson.core.JsonProcessingException;
import tools.vitruv.framework.remote.modules.vsums.model.manager.SelectorManager;
import tools.vitruv.framework.remote.modules.vsums.model.manager.ViewManager;
import tools.vitruv.framework.remote.modules.vsums.model.manager.VsumManager;
import tools.vitruv.framework.remote.modules.vsums.model.services.ViewService;
import tools.vitruv.framework.remote.modules.vsums.model.wrapper.ViewWrapper;
import tools.vitruv.framework.remote.modules.vsums.usecases.InconsistencyUseCases;
import tools.vitruv.framework.remote.modules.vsums.usecases.dtos.OpenViewRequestBody;
import tools.vitruv.framework.remote.modules.vsums.usecases.strategies.ViewUseCasesStrategy;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.eclipse.emf.ecore.EObject;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

/**
 * Implementation of the {@link ViewUseCasesStrategy} interface that provides
 * mechanisms for managing views and their associated selectors within a system.
 * This implementation integrates with various managers and services to handle
 * the lifecycle of views and selectors, including creation, opening, updating,
 * committing resource sets, and closing views.
 */
@Component
@RequiredArgsConstructor
public class InternalViewStrategyImpl implements ViewUseCasesStrategy {
    private final VsumManager vsumManager;
    private final ViewService viewService;
    private final SelectorManager selectorManager;
    private final ViewManager viewManager;
    private final InconsistencyUseCases inconsistencyUseCases;

    /**
     * {@inheritDoc}
     */
    @Override
    public CreateSelectorResponse createSelector(UUID vsumId, String viewTypeName) {
        val vsumWrapper = vsumManager.getVsum(vsumId);
        val selectorWrapper = viewService.createSelector(vsumWrapper, viewTypeName);
        selectorManager.addSelector(selectorWrapper);

        String serializedObjects;
        try {
            serializedObjects = vsumWrapper.jsonMapper().serialize(selectorWrapper.resource());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return new CreateSelectorResponse(selectorWrapper.selectorId(), serializedObjects);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OpenViewResponse openView(OpenViewRequestBody body) {
        val selectorWrapper = selectorManager.getSelector(body.selectorId());

        if (selectorWrapper == null) {
            throw new RuntimeException("Selector not found: " + body.selectorId());
        }

        val selectedObjects = Arrays.stream(body.selectedObjectIds()).map(selectedObjectId ->
                selectorManager
                        .getSelector(body.selectorId())
                        .selectableObjects()
                        .get(selectedObjectId)
        ).toArray(EObject[]::new);

        if (selectedObjects.length == 0) {
            throw new RuntimeException("No selected objects found for selector: " + body.selectorId());
        }

        UUID vsumId = selectorWrapper.vsumWrapper().info().getId();
        if (inconsistencyUseCases.hasOpenForVsum(vsumId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "VSUM has an open inconsistency; resolve it in the Hub before opening this VSUM again");
        }
        // Drop stale leftover views (e.g. after failed closes) so reopen does not hit recorder conflicts.
        viewManager.removeAndCloseViewsForVsum(vsumId);

        val viewWrapper = viewService.openView(selectorWrapper, selectedObjects);

        viewManager.addView(viewWrapper);
        selectorManager.removeSelector(body.selectorId());

        final String encodedResourceSet;
        try {
            encodedResourceSet = viewWrapper.vsumWrapper().jsonMapper().serialize(viewWrapper.resourceSet());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new OpenViewResponse(viewWrapper.viewId(), encodedResourceSet);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String commitResourceSet(UUID viewId, String resourceSetBody) {
        val viewWrapper = getViewOrThrow(viewId);
        val change = viewService.commitResourceSet(viewWrapper, resourceSetBody);

        if (change == null) {
            return "[]";
        }

        try {
            return viewWrapper.vsumWrapper().jsonMapper().serialize(change);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String update(UUID viewId) {
        val viewWrapper = getViewOrThrow(viewId);
        return viewService.update(viewWrapper);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void closeView(UUID viewId) {
        viewManager.removeAndCloseView(viewId);
    }

    private ViewWrapper getViewOrThrow(UUID viewId) {
        val viewWrapper = viewManager.getView(viewId);
        if (viewWrapper == null) {
            throw new IllegalArgumentException("View not found: " + viewId);
        }
        return viewWrapper;
    }
}
