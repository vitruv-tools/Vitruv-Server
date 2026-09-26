package tools.vitruv.framework.remote.modules.vsums.model.wrapper;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emfcloud.jackson.resource.JsonResource;
import tools.vitruv.framework.views.ViewSelector;

import java.util.Map;
import java.util.UUID;

/**
 * A record that serves as a wrapper for managing a ViewSelector and its associated metadata and resources
 * in the context of a Virtual System Under Maintenance (VSUM).
 * <p>
 * This record encapsulates:
 * - `selectorId`: A unique identifier for the SelectorWrapper instance.
 * - `selector`: The {@link ViewSelector} instance responsible for managing view selections and their synchronization.
 * - `selectableObjects`: A mapping of unique identifiers to {@link EObject} instances that represent objects
 * eligible for selection within the associated ViewSelector.
 * - `resource`: The {@link Resource} (provided by {@link JsonResource}) associated with the selection operations.
 * - `vsumWrapper`: A {@link VsumWrapper} instance that contains metadata, the virtual model, and related utilities
 * tied to the Virtual System Under Maintenance (VSUM).
 * <p>
 * Typically used for managing relationships between selectable objects, their views, and the underlying virtual model
 * in the VSUM framework.
 */
public record SelectorWrapper(
    UUID selectorId,
    ViewSelector selector,
    Map<UUID, EObject> selectableObjects,
    JsonResource resource,
    VsumWrapper vsumWrapper
) {
}
