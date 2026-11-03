package tools.vitruv.framework.remote.common.rest.constraints;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import org.eclipse.emf.ecore.resource.ResourceSet;

/**
 * The result of validating constraints of the virtual model.
 *
 * @param satisfied whether all validated constraints are satisfied
 * @param results the results, one per validated constraint
 * @param models the models containing violating elements, in which every element is identified by
 *     its UUID ({@code _id}); {@code null} unless requested
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ConstraintValidationResponse(
    boolean satisfied, List<ConstraintResultDto> results, ResourceSet models) {}
