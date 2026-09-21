package tools.vitruv.framework.remote.modules.vsums.usecases.dtos;

import tools.vitruv.framework.remote.modules.vsums.async.AsyncTaskState;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record PropagationTaskStatusResponse(
        UUID taskId,
        UUID viewId,
        AsyncTaskState state,
        Instant createdAt,
        Instant completedAt,
        String error,
        Map<String, Object> interaction,
        String result
) {
}
