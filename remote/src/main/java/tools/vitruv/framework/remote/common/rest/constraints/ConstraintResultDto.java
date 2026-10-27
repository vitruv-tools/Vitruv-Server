package tools.vitruv.framework.remote.common.rest.constraints;

import java.util.List;

/**
 * The result of validating a single constraint.
 *
 * @param constraintId the id of the constraint
 * @param name the name of the constraint
 * @param status {@code SATISFIED}, {@code VIOLATED} or {@code ERROR}
 * @param severity the default severity of the constraint
 * @param violations the violating elements
 * @param errors the reasons why the constraint could not be checked
 */
public record ConstraintResultDto(
    String constraintId,
    String name,
    String status,
    String severity,
    List<ViolationDto> violations,
    List<String> errors) {}
