package tools.vitruv.framework.remote.modules.vsums.usecases.dtos;

import java.time.Instant;
import java.util.UUID;

/**
 * Latest view snapshot for Hub visualization (same resource set JSON as POST /v1/views).
 */
public record InconsistencyModelSnapshotResponse(
        UUID inconsistencyId,
        UUID vsumId,
        String vsumName,
        String metamodelName,
        String viewTypeName,
        Instant snapshotTimestamp,
        String note,
        String encodedResourceSet
) {
}
