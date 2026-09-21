package tools.vitruv.framework.remote.modules.vsums.usecases.dtos;

public record RecordInconsistencyResolutionRequest(
        String resolvedBy,
        String choice,
        String comment
) {
}
