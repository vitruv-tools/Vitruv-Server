package tools.vitruv.framework.remote.common.rest.constraints;

import java.util.List;
import tools.vitruv.framework.vsum.constraints.ConstraintDescriptor;

/**
 * A constraint of the virtual model.
 *
 * @param id the unique id of the constraint
 * @param name the human-readable name
 * @param contextType the type the constraint is defined for, may be {@code null}
 * @param kind the kind, e.g. {@code INVARIANT}
 * @param severity the default severity of violations, e.g. {@code CRITICAL}
 * @param language the constraint language, e.g. {@code vitruvocl}
 * @param source the textual source of the constraint, may be {@code null}
 * @param metamodels the names of the metamodels the constraint reads, the metamodel of the context
 *     type first; empty if unknown, never {@code null}
 */
public record ConstraintDto(
    String id,
    String name,
    String contextType,
    String kind,
    String severity,
    String language,
    String source,
    List<String> metamodels) {

  /** Makes the metamodels immutable; answers of servers without metamodels yield an empty list. */
  public ConstraintDto {
    metamodels = metamodels == null ? List.of() : List.copyOf(metamodels);
  }

  /**
   * Creates the transfer object for the given constraint.
   *
   * @param descriptor the constraint
   * @return the transfer object
   */
  public static ConstraintDto from(ConstraintDescriptor descriptor) {
    return new ConstraintDto(
        descriptor.id(),
        descriptor.name(),
        descriptor.contextType(),
        descriptor.kind().name(),
        descriptor.severity().name(),
        descriptor.language(),
        descriptor.source(),
        List.copyOf(descriptor.metamodels()));
  }
}
