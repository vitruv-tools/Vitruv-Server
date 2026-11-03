package tools.vitruv.framework.remote.common.rest.constraints;

import java.util.List;
import tools.vitruv.framework.vsum.constraints.ConstraintDiagnostic;

/**
 * The body of an error response of the constraint endpoints.
 *
 * @param error the description of the error
 * @param diagnostics the problems of an invalid constraint source, empty for other errors
 */
public record ConstraintErrorDto(String error, List<DiagnosticDto> diagnostics) {

  /**
   * A problem of an invalid constraint source.
   *
   * @param line the line of the problem, starting at 1
   * @param column the column of the problem, starting at 0
   * @param message the description of the problem
   */
  public record DiagnosticDto(int line, int column, String message) {

    /**
     * Creates the transfer object for the given diagnostic.
     *
     * @param diagnostic the diagnostic
     * @return the transfer object
     */
    public static DiagnosticDto from(ConstraintDiagnostic diagnostic) {
      return new DiagnosticDto(diagnostic.line(), diagnostic.column(), diagnostic.message());
    }
  }
}
