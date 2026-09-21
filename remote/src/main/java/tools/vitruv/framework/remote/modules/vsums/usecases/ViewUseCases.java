package tools.vitruv.framework.remote.modules.vsums.usecases;

import tools.vitruv.framework.remote.modules.vsums.async.AsyncPropagationService;
import tools.vitruv.framework.remote.modules.vsums.usecases.dtos.OpenViewRequestBody;
import tools.vitruv.framework.remote.modules.vsums.usecases.dtos.StartAsyncUpdateResponse;
import tools.vitruv.framework.remote.modules.vsums.usecases.strategies.UseCasesStrategySelector;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * The ViewUseCases class encapsulates the business logic related to managing
 * views and selectors within the VSUM module. It provides functionalities
 * such as creating selectors, opening views, committing resource sets,
 * updating views, and closing views. The class relies on the
 * UseCasesStrategySelector to delegate operations to the relevant strategy
 * based on the unique identifiers provided.
 */
@Service
@RequiredArgsConstructor
public class ViewUseCases {
    private final UseCasesStrategySelector useCasesStrategySelector;
    private final AsyncPropagationService asyncPropagationService;

    /**
     * Creates a selector representation for a given VSUM and view type.
     * The method generates a JSON string containing the selector ID and
     * the associated selectable objects. If the selectable object's JSON
     * initially starts with a '{', it will be wrapped in square brackets
     * to ensure proper list formatting.
     *
     * @param vsumId the unique identifier of the VSUM for which the selector
     *               is to be created.
     * @param viewTypeName the name of the view type that defines how the
     *                     data should be presented or interacted with.
     * @return a JSON string representing the created selector, containing
     *         fields for the selector ID and the list of selectable objects.
     */
    public String createSelector(UUID vsumId, String viewTypeName) {
        val result = useCasesStrategySelector.getViewStrategy(vsumId).createSelector(vsumId, viewTypeName);
        var serializedObjects = result.selectableObjects();

        if (serializedObjects.startsWith("{")) {
            serializedObjects = "[" + serializedObjects + "]";
        }

        return "{\"id\":\"" + result.selectorId() + "\",\"selectableObjects\":" + serializedObjects + "}";
    }

    /**
     * Opens a view for the specified VSUM with the provided request body details.
     * The method returns a JSON representation containing the view ID and the
     * associated encoded resource set.
     *
     * @param body the request body containing the VSUM ID, selector ID, and an array
     *             of selected object IDs used to open the view.
     * @return a JSON string containing the view ID and the encoded resource set
     *         associated with the opened view.
     */
    public String openView(OpenViewRequestBody body) {
        val result = useCasesStrategySelector.getViewStrategy(body.vsumId()).openView(body);
        return "{\"id\":\"" + result.viewId() + "\",\"resourceSet\":" + result.encodedResourceSet() + "}";
    }

    /**
     * Commits a resource set for a specific view.
     * The method delegates the commit operation to a strategy retrieved based on the given view ID.
     *
     * @param viewId the unique identifier of the view for which the resource set should be committed
     * @param resourceSetBody the resource set data to be committed, represented as a string
     * @return a serialized response from the strategy indicating the result of the commit operation
     */
    public String commitResourceSet(UUID viewId, String resourceSetBody) {
        return useCasesStrategySelector.getViewStrategy(viewId).commitResourceSet(viewId, resourceSetBody);
    }

    /**
     * Applies the update of a specific view identified by its unique identifier.
     * This method delegates the update operation to a corresponding strategy
     * based on the provided view ID.
     *
     * @param viewId the unique identifier of the view to be updated
     * @return a string representation of the update result, as provided by the
     *         appropriate strategy
     */
    public String update(UUID viewId) {
        return useCasesStrategySelector.getViewStrategy(viewId).update(viewId);
    }

    /**
     * Starts asynchronous propagation for a view that already has committed changes.
     * The returned task ID can be polled via {@link PropagationTaskUseCases}.
     *
     * @param viewId the unique identifier of the view to propagate asynchronously
     * @return a response containing the async task ID
     */
    public StartAsyncUpdateResponse startAsyncUpdate(UUID viewId) {
        return startAsyncUpdate(viewId, null);
    }

    /**
     * Starts async propagation. When {@code resourceSetBody} is set, the resource set is committed on
     * the propagation worker thread (same thread as {@code view.update()}) so reactions that prompt
     * the user run with an active async task.
     */
    public StartAsyncUpdateResponse startAsyncUpdate(UUID viewId, String resourceSetBody) {
        return new StartAsyncUpdateResponse(asyncPropagationService.startUpdate(viewId, resourceSetBody));
    }

    /**
     * Closes a specific view identified by its unique identifier.
     * This method delegates the close operation to a corresponding strategy
     * retrieved based on the provided view ID.
     *
     * @param viewId the unique identifier of the view to be closed
     */
    public void closeView(UUID viewId) {
        useCasesStrategySelector.getViewStrategy(viewId).closeView(viewId);
    }
}
