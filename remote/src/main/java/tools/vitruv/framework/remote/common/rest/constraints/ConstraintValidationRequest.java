package tools.vitruv.framework.remote.common.rest.constraints;

import java.util.List;

/**
 * A request to validate constraints of the virtual model.
 *
 * @param constraintIds the ids of the constraints to validate; {@code null} validates all
 *     constraints, an empty list none
 * @param includeModels whether the models containing violating elements are returned, annotated
 *     with the UUIDs of their elements; {@code null} means {@code false}
 */
public record ConstraintValidationRequest(List<String> constraintIds, Boolean includeModels) {

  /**
   * Checks whether the affected models are requested.
   *
   * @return {@code true} if the models containing violating elements should be returned
   */
  public boolean includesModels() {
    return Boolean.TRUE.equals(includeModels);
  }
}
