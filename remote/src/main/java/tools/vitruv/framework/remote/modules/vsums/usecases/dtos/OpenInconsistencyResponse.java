package tools.vitruv.framework.remote.modules.vsums.usecases.dtos;

import tools.vitruv.framework.remote.modules.vsums.model.entities.OpenInconsistencyState;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record OpenInconsistencyResponse(
    UUID id,
    UUID vsumId,
    UUID taskId,
    UUID viewId,
    String vsumName,
    String metamodelName,
    String title,
    String message,
    Map<String, Object> interaction,
    OpenInconsistencyState state,
    Instant createdAt,
    Instant resolvedAt,
    String resolvedBy,
    String resolutionChoice,
    String resolutionComment
) {
}
