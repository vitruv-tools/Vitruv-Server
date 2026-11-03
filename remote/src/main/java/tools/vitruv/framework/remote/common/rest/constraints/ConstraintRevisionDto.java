package tools.vitruv.framework.remote.common.rest.constraints;

/**
 * The revision of the constraints of the virtual model. It changes whenever a constraint is
 * created, updated or deleted, so clients can detect changes made by others.
 *
 * @param revision the revision, also sent as {@code ETag} header
 */
public record ConstraintRevisionDto(String revision) {}
