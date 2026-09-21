package tools.vitruv.framework.remote.modules.vsums.model.wrapper;

import org.eclipse.emf.ecore.resource.ResourceSet;
import tools.vitruv.framework.views.View;

import java.util.UUID;

/**
 * A record that serves as a wrapper for encapsulating information and resources associated with a View
 * in the context of a Virtual Model (VSUM).
 * <p>
 * This record combines:
 * - {@code viewId}: A unique identifier for the associated View.
 * - {@code view}: The core {@link View} instance linked with the VSUM and its related components.
 * - {@code resourceSet}: The {@link ResourceSet} that contains the resources related to the View.
 * - {@code vsumWrapper}: A {@link VsumWrapper} instance encapsulating metadata and components related to the VSUM.
 * <p>
 * Typically utilized to group these components together to facilitate management and interaction within
 * View-related operations.
 */
public record ViewWrapper(
        UUID viewId,
        View view,
        ResourceSet resourceSet,
        VsumWrapper vsumWrapper
) {
}
