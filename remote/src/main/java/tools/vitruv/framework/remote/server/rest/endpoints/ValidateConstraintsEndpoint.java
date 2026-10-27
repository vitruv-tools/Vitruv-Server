package tools.vitruv.framework.remote.server.rest.endpoints;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import tools.vitruv.framework.remote.common.json.JsonMapper;
import tools.vitruv.framework.remote.common.rest.constants.ContentType;
import tools.vitruv.framework.remote.common.rest.constraints.ConstraintValidationRequest;
import tools.vitruv.framework.remote.common.rest.constraints.ConstraintValidationResponse;
import tools.vitruv.framework.remote.server.exception.ServerHaltingException;
import tools.vitruv.framework.remote.server.http.HttpWrapper;
import tools.vitruv.framework.remote.server.rest.PostEndpoint;
import tools.vitruv.framework.vsum.VirtualModel;
import tools.vitruv.framework.vsum.constraints.ConstraintDescriptor;

/**
 * This endpoint validates constraints of the virtual model. The optional body is a {@link
 * ConstraintValidationRequest}; without body, all constraints are validated and no models are
 * returned.
 *
 * <p>Responds with 404 for unknown constraint ids and with 400 for malformed requests.
 */
public class ValidateConstraintsEndpoint implements PostEndpoint {
  private static final String ENDPOINT_METRIC_NAME = "vitruv.server.rest.constraints.validation";

  private final VirtualModel model;
  private final JsonMapper mapper;
  private final ConstraintValidationExporter exporter;

  /**
   * Creates a new ValidateConstraintsEndpoint.
   *
   * @param model The virtual model whose constraints are validated.
   * @param mapper The JSON mapper to use.
   */
  public ValidateConstraintsEndpoint(VirtualModel model, JsonMapper mapper) {
    this.model = model;
    this.mapper = mapper;
    this.exporter = new ConstraintValidationExporter(model, mapper.getIdTransformation());
  }

  @Override
  public String process(HttpWrapper wrapper) throws ServerHaltingException {
    ConstraintValidationRequest request = readRequest(wrapper);
    checkConstraintIds(request.constraintIds());

    var timer = Timer.start(Metrics.globalRegistry);
    String outcome = "failure";
    ConstraintValidationResponse response;
    try {
      response = exporter.validate(request.constraintIds(), request.includesModels());
      outcome = "success";
    } catch (RuntimeException e) {
      throw internalServerError("Constraint validation failed: " + e.getMessage());
    } finally {
      timer.stop(Metrics.timer(ENDPOINT_METRIC_NAME, "validation", outcome));
    }

    wrapper.setContentType(ContentType.APPLICATION_JSON);
    try {
      return mapper.serialize(response);
    } catch (JsonProcessingException e) {
      throw internalServerError(e.getMessage());
    }
  }

  private ConstraintValidationRequest readRequest(HttpWrapper wrapper) {
    ConstraintValidationRequest request = null;
    try {
      String body = wrapper.getRequestBodyAsString();
      if (body != null && !body.isBlank()) {
        request = mapper.deserialize(body, ConstraintValidationRequest.class);
      }
    } catch (JsonProcessingException e) {
      throw new ServerHaltingException(
          HTTP_BAD_REQUEST, "Invalid request: " + e.getOriginalMessage());
    } catch (IOException e) {
      throw new ServerHaltingException(HTTP_BAD_REQUEST, "Could not read request body");
    }
    return request != null ? request : new ConstraintValidationRequest(null, false);
  }

  private void checkConstraintIds(List<String> constraintIds) {
    if (constraintIds == null) {
      return;
    }
    Set<String> knownIds =
        model.getConstraints().stream().map(ConstraintDescriptor::id).collect(Collectors.toSet());
    for (String id : constraintIds) {
      if (id == null || id.isBlank()) {
        throw new ServerHaltingException(HTTP_BAD_REQUEST, "Constraint ids must not be empty");
      }
      if (!knownIds.contains(id)) {
        throw new ServerHaltingException(HTTP_NOT_FOUND, "Constraint " + id + " not found");
      }
    }
  }
}
