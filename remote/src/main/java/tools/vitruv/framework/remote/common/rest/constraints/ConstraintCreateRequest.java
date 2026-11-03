package tools.vitruv.framework.remote.common.rest.constraints;

/**
 * A request to create a constraint of the virtual model.
 *
 * @param language the language of the constraint, e.g. {@code vitruvocl}
 * @param source the source of exactly one constraint
 */
public record ConstraintCreateRequest(String language, String source) {}
