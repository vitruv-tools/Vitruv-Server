package tools.vitruv.framework.remote.server.rest.endpoints;

import java.util.List;
import java.util.Optional;
import tools.vitruv.framework.remote.common.json.JsonMapper;
import tools.vitruv.framework.remote.common.rest.constraints.ConstraintDto;
import tools.vitruv.framework.remote.server.http.HttpWrapper;
import tools.vitruv.framework.remote.server.rest.GetEndpoint;
import tools.vitruv.framework.vsum.VirtualModel;

/**
 * This endpoint returns all constraints registered at the virtual model, or the constraint whose id
 * is given in the path ({@code /vsum/constraints/{id}}).
 */
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
    Optional<String> id = ConstraintRequests.idOf(wrapper);
    // read the constraints and their revision consistently
    synchronized (model) {
      if (id.isEmpty()) {
        List<ConstraintDto> constraints =
            model.getConstraints().stream().map(ConstraintDto::from).toList();
        return ConstraintRequests.respond(wrapper, model, mapper, constraints);
      }
      ConstraintDto constraint =
          model
              .getConstraint(id.get())
              .map(ConstraintDto::from)
              .orElseThrow(() -> ConstraintRequests.notFound(id.get(), mapper));
      return ConstraintRequests.respond(wrapper, model, mapper, constraint);
    }
  }
}
