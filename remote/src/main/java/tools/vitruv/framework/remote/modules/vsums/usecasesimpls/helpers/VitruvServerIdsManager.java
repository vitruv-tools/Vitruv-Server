package tools.vitruv.framework.remote.modules.vsums.usecasesimpls.helpers;

import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Caches a set of UUIDs that are managed by the VitruvServer.
 * Provides mechanisms to add new IDs and check for their existence.
 */
@Component
public class VitruvServerIdsManager {

    private final static UUID VITRUV_SERVER_VSUM_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final Set<UUID> ids = new HashSet<>(List.of(VITRUV_SERVER_VSUM_ID));

    /**
     * Adds the specified UUID to the internal set of IDs.
     *
     * @param id the UUID to be added
     */
    public void addId(UUID id) {
        ids.add(id);
    }

    /**
     * Checks if the specified UUID is present in the internal set of IDs.
     *
     * @param id the UUID to check for presence
     * @return true if the UUID is present in the set, false otherwise
     */
    public boolean hasId(UUID id) {
        return ids.contains(id);
    }
}
