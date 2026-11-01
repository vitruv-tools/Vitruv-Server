package tools.vitruv.framework.remote.server.rest.endpoints;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import tools.vitruv.framework.remote.common.json.JsonMapper;
import tools.vitruv.framework.remote.common.rest.constraints.ConstraintStatusDto;
import tools.vitruv.framework.remote.server.exception.ServerHaltingException;
import tools.vitruv.framework.remote.server.http.HttpWrapper;
import tools.vitruv.framework.remote.server.rest.GetEndpoint;
import tools.vitruv.framework.vsum.VirtualModel;
import tools.vitruv.framework.vsum.constraints.ConstraintValidationReport;
import tools.vitruv.framework.vsum.constraints.ConstraintValidationResult;

/**
 * This endpoint validates constraints of the virtual model and returns only their status, without
 * violations or models, so clients can cheaply poll the consistency of the virtual model. The
 * optional, repeatable query parameter {@code id} restricts the validation to some constraints.
 *
 * <p>Responds with 404 for unknown constraint ids and with 400 for empty ones.
 */
public class ConstraintStatusEndpoint implements GetEndpoint {
  private static final String ENDPOINT_METRIC_NAME = "vitruv.server.rest.constraints.status";
  private static final String ID_PARAMETER = "id";

  private final VirtualModel model;
  private final JsonMapper mapper;

  /**
   * Creates a new ConstraintStatusEndpoint.
   *
   * @param model The virtual model whose constraints are validated.
   * @param mapper The JSON mapper to serialize the response.
   */
  public ConstraintStatusEndpoint(VirtualModel model, JsonMapper mapper) {
    this.model = model;
    this.mapper = mapper;
  }

  @Override
  public String process(HttpWrapper wrapper) throws ServerHaltingException {
    List<String> constraintIds = ConstraintRequests.queryValues(wrapper, ID_PARAMETER);
    var timer = Timer.start(Metrics.globalRegistry);
    String outcome = "failure";
    // the revision must belong to the validated constraints
    synchronized (model) {
      ConstraintRequests.checkKnownIds(model, constraintIds);
      ConstraintValidationReport report;
      try {
        report =
            constraintIds == null
                ? model.validateConstraints()
                : model.validateConstraints(constraintIds);
        outcome = "success";
      } catch (RuntimeException e) {
        throw internalServerError("Constraint validation failed: " + e.getMessage());
      } finally {
        timer.stop(Metrics.timer(ENDPOINT_METRIC_NAME, "validation", outcome));
      }
      return ConstraintRequests.respond(
          wrapper, model, mapper, toStatus(report, ConstraintRequests.revisionOf(model)));
    }
  }

  private static ConstraintStatusDto toStatus(ConstraintValidationReport report, String revision) {
    Map<String, String> statuses = new LinkedHashMap<>();
    for (ConstraintValidationResult result : report.results()) {
      statuses.put(result.descriptor().id(), result.status().name());
    }
    return new ConstraintStatusDto(revision, report.isSatisfied(), statuses);
  }
}
