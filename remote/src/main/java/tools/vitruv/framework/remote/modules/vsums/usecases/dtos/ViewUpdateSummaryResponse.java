package tools.vitruv.framework.remote.modules.vsums.usecases.dtos;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ViewUpdateSummaryResponse(
    UUID id,
    UUID vsumId,
    String viewTypeName,
    List<String> selectedObjectEClassNames,
    Instant timestamp,
    int resourceSetLength
) {
}
