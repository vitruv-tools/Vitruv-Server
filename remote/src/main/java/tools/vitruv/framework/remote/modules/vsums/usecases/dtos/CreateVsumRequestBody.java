package tools.vitruv.framework.remote.modules.vsums.usecases.dtos;

public record CreateVsumRequestBody(
        String metamodelName,
        String name,
        String description
) {
}
