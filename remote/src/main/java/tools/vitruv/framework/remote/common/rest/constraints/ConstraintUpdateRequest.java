package tools.vitruv.framework.remote.common.rest.constraints;

/**
 * A request to replace the source of a constraint of the virtual model.
 *
 * @param source the new source, which must define a constraint with the same id
 */
public record ConstraintUpdateRequest(String source) {}
