package tools.vitruv.framework.remote.common.rest.constraints;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The status of constraints of the virtual model, without violations or models, e.g. for clients
 * that poll the consistency of the virtual model.
 *
 * @param revision the revision of the constraints, as in the {@code ETag} of the constraint
 *     endpoints; it changes when constraints are created, updated or deleted, not when models
 *     change
 * @param satisfied whether all listed constraints are satisfied
 * @param constraints the status per constraint id: {@code SATISFIED}, {@code VIOLATED} or {@code
 *     ERROR}, in the order of the constraints of the virtual model
 */
public record ConstraintStatusDto(
    String revision, boolean satisfied, Map<String, String> constraints) {

  /** Makes the statuses immutable and keeps their order. */
  public ConstraintStatusDto {
    constraints =
        constraints == null
            ? Map.of()
            : Collections.unmodifiableMap(new LinkedHashMap<>(constraints));
  }
}
