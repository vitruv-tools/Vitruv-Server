package tools.vitruv.framework.remote.modules.vsums.usecases.dtos;

import java.util.UUID;

public record OpenViewRequestBody(
        UUID vsumId,
        UUID selectorId,
        UUID[] selectedObjectIds
) {
}
