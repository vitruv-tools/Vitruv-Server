package tools.vitruv.framework.remote.modules.vsums.controller;

import tools.vitruv.framework.remote.modules.vsums.usecases.ViewUseCases;
import tools.vitruv.framework.remote.modules.vsums.usecases.dtos.OpenViewRequestBody;
import tools.vitruv.framework.remote.modules.vsums.usecases.dtos.StartAsyncUpdateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Controller for managing views.
 * Provides endpoints to perform operations such as opening, committing changes, updating,
 * and closing views.
 * <p>
 * The operations are delegated to the underlying business logic defined in {@code ViewUseCases}.
 */
@RestController
@RequestMapping("/v1/views")
@RequiredArgsConstructor
public class ViewController {
    private final ViewUseCases viewUseCases;

    /**
     * Opens a view for a specified VSUM using the provided
     * details in the request body. The method delegates the operation to the underlying
     * business logic and returns a JSON string containing the view ID and the associated
     * encoded resource set.
     *
     * @param body the request body containing the following details:
     *             <ul>
     *             - vsumId: the unique identifier of the VSUM for which the view is to be opened
     *             - selectorId: the unique identifier of the selector to be used
     *             - selectedObjectIds: an array of unique identifiers for selected objects within the view
     *             </ul>
     * @return a JSON string containing the view ID and the encoded resource set associated
     * with the opened view
     */
    @PostMapping(produces = APPLICATION_JSON_VALUE)
    public String openView(@RequestBody OpenViewRequestBody body) {
        return viewUseCases.openView(body);
    }


    /**
     * Commits changes to a specific view by submitting a resource set.
     * This method processes the resource set data for the view identified by the given UUID.
     *
     * @param viewId          the unique identifier of the view to which the changes should be committed
     * @param resourceSetBody the resource set data to be committed, represented as a JSON string
     * @return a JSON string representing the result of the commit operation
     */
    @PutMapping(value = "/{viewId}", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public String commitChangesOfView(@PathVariable UUID viewId, @RequestBody String resourceSetBody) {
        return viewUseCases.commitResourceSet(viewId, resourceSetBody);
    }


    /**
     * Applies updates of a specific view identified by its unique ID.
     * This method triggers the update process and delegates the operation
     * to the underlying business logic associated with the view.
     *
     * @param viewId the unique identifier of the view to be updated
     * @return a JSON string representing the outcome of the update operation
     */
    @PostMapping(value = "/{viewId}/apply-update", produces = APPLICATION_JSON_VALUE)
    public String update(@PathVariable UUID viewId) {
        return viewUseCases.update(viewId);
    }

    /**
     * Starts asynchronous change propagation for a view.
     * Returns immediately with a task ID that can be polled via {@code GET /v1/tasks/{taskId}}.
     * <p>
     * When a JSON resource-set body is supplied, commit and update both run on the propagation worker
     * thread (required for user interactions triggered during commit-time reactions). When omitted,
     * only {@code view.update()} runs (changes must already be committed via {@code PUT}).
     *
     * @param viewId the unique identifier of the view to propagate
     * @param resourceSetBody optional resource set to commit before propagating
     * @return a response containing the async task ID
     */
    @PostMapping(value = "/{viewId}/apply-update/async", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public StartAsyncUpdateResponse startAsyncUpdate(
            @PathVariable UUID viewId, @RequestBody(required = false) String resourceSetBody) {
        return viewUseCases.startAsyncUpdate(viewId, resourceSetBody);
    }

    /**
     * Closes a specific view identified by its unique ID.
     * This method delegates the close operation to the underlying business logic
     * associated with the view.
     *
     * @param viewId the unique identifier of the view to be closed
     */
    @DeleteMapping("/{viewId}")
    public void closeView(@PathVariable UUID viewId) {
        viewUseCases.closeView(viewId);
    }
}
