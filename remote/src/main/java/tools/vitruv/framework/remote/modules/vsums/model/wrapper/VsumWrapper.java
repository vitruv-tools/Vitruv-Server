package tools.vitruv.framework.remote.modules.vsums.model.wrapper;

import tools.vitruv.framework.remote.common.utils.json.JsonMapper;
import tools.vitruv.framework.remote.modules.vsums.model.entities.VsumInfo;
import tools.vitruv.framework.vsum.VirtualModel;

/**
 * A wrapper class encapsulating relevant components and metadata associated with a Virtual Model (VSUM).
 * <p>
 * This record combines:
 * - {@link VsumInfo}: Metadata information about the VSUM, including its name, description, and meta-model.
 * - {@link VirtualModel}: The core virtual model representation tied to the VSUM.
 * - {@link JsonMapper}: A utility for serializing and deserializing VSUM-related objects into JSON format.
 * <p>
 * Typically used to group these components for easy handling and interaction within VSUM-related operations.
 */
public record VsumWrapper(
        VsumInfo info,
        VirtualModel virtualModel,
        JsonMapper jsonMapper
) {
}
