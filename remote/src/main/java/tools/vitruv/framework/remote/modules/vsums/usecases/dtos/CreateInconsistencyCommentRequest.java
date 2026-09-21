package tools.vitruv.framework.remote.modules.vsums.usecases.dtos;

public record CreateInconsistencyCommentRequest(
        String author,
        String body
) {
}
