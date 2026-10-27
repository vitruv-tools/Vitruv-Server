package tools.vitruv.framework.remote.server.rest.endpoints;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import tools.vitruv.framework.remote.common.json.JsonMapper;
import tools.vitruv.framework.remote.common.rest.constants.ContentType;
import tools.vitruv.framework.remote.common.rest.constraints.ConstraintDto;
import tools.vitruv.framework.remote.server.http.HttpWrapper;
import tools.vitruv.framework.remote.server.rest.GetEndpoint;
import tools.vitruv.framework.vsum.VirtualModel;

/** This endpoint returns all constraints registered at the virtual model. */
public class ConstraintsEndpoint implements GetEndpoint {
  private final VirtualModel model;
  private final JsonMapper mapper;

  /**
   * Creates a new ConstraintsEndpoint.
   *
   * @param model The virtual model providing the constraints.
   * @param mapper The JSON mapper to serialize the response.
   */
  public ConstraintsEndpoint(VirtualModel model, JsonMapper mapper) {
    this.model = model;
    this.mapper = mapper;
  }

  @Override
  public String process(HttpWrapper wrapper) {
    List<ConstraintDto> constraints =
        model.getConstraints().stream().map(ConstraintDto::from).toList();
    wrapper.setContentType(ContentType.APPLICATION_JSON);
    try {
      return mapper.serialize(constraints);
    } catch (JsonProcessingException e) {
      throw internalServerError(e.getMessage());
    }
  }
}
