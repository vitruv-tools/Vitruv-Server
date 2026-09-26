package tools.vitruv.framework.remote.modules.vsums.usecases.dtos;

import java.util.UUID;

public record VsumInfoResponseBody(
    UUID id,
    String metamodelName,
    String name,
    String description
) {
}
