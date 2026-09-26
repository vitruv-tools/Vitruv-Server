package tools.vitruv.framework.remote.modules.vsums.usecases.strategies;

import tools.vitruv.framework.remote.modules.vsums.usecases.dtos.OpenViewRequestBody;

import java.util.UUID;

public interface ViewUseCasesStrategy {
  record CreateSelectorResponse(UUID selectorId, String selectableObjects) {
  }

  /**
   * Creates a selector for the given VSUM ID and view type name.
   * The selector encapsulates data for selectable objects based on
   * the provided VSUM and view type.
   *
   * @param vsumId       the unique identifier of the VSUM for which the selector
   *                     is to be created
   * @param viewTypeName the name of the view type that defines the specifics
   *                     of the selector and its associated selectable objects
   * @return a CreateSelectorResponse containing the ID of the created selector
   * and a serialized representation of the selectable objects
   */
  CreateSelectorResponse createSelector(UUID vsumId, String viewTypeName);

  record OpenViewResponse(UUID viewId, String encodedResourceSet) {
  }

  /**
   * Opens a view for a specific VSUM using the provided request body details.
   * The method allows creating a view based on the associated selector ID and
   * the selection of object IDs supplied in the request body.
   *
   * @param body the request body containing the VSUM ID, selector ID, and an array
   *             of selected object IDs required to initialize the view.
   * @return an OpenViewResponse containing the unique ID of the opened view and
   * an encoded representation of the resource set associated with the view.
   */
  OpenViewResponse openView(OpenViewRequestBody body);

  /**
   * Commits a resource set for a specific view identified by the given view ID.
   * The method updates or finalizes the state of the resource set associated
   * with the view using the provided serialized resource set body.
   *
   * @param viewId          the unique identifier of the view for which the resource set
   *                        is being committed
   * @param resourceSetBody the serialized representation of the resource set
   *                        to be committed
   * @return a serialized string representation of the updated or committed
   * resource set
   */
  String commitResourceSet(UUID viewId, String resourceSetBody);

  /**
   * Applies the update of a specific view identified by its unique identifier.
   * This method modifies the view and applies necessary changes as part of the
   * update process.
   *
   * @param viewId the unique identifier of the view to be updated
   * @return a serialized string representation of the updated view state
   */
  String update(UUID viewId);

  /**
   * Closes the view identified by the provided unique identifier. This operation
   * is used to terminate or clean up resources associated with the specified view.
   * The exact behavior of this method may depend on the implementation of the view
   * management strategy.
   *
   * @param viewId the unique identifier of the view to be closed
   */
  void closeView(UUID viewId);
}
