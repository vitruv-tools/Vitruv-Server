package tools.vitruv.framework.remote.modules.vsums.usecases.dtos;

import java.time.Instant;
import java.util.UUID;

public record InconsistencyCommentResponse(
        UUID id,
        UUID inconsistencyId,
        String author,
        String body,
        Instant createdAt
) {
}
