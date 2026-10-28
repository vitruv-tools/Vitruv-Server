package tools.vitruv.framework.remote.server.rest.endpoints;

import tools.vitruv.framework.remote.common.json.JsonMapper;
import tools.vitruv.framework.remote.common.rest.constraints.ConstraintDto;
import tools.vitruv.framework.remote.common.rest.constraints.ConstraintUpdateRequest;
import tools.vitruv.framework.remote.server.http.HttpWrapper;
import tools.vitruv.framework.remote.server.rest.PutEndpoint;
import tools.vitruv.framework.vsum.VirtualModel;
import tools.vitruv.framework.vsum.constraints.ConstraintDescriptor;

/**
 * This endpoint replaces the source of the constraint whose id is given in the path, including
 * constraints the virtual model was built with. An {@code If-Match} header restricts the change to
 * the given revision of the constraints.
 */
public class UpdateConstraintEndpoint implements PutEndpoint {
  private final VirtualModel model;
  private final JsonMapper mapper;

  /**
   * Creates a new UpdateConstraintEndpoint.
   *
   * @param model The virtual model whose constraints are updated.
   * @param mapper The JSON mapper to use.
   */
  public UpdateConstraintEndpoint(VirtualModel model, JsonMapper mapper) {
    this.model = model;
    this.mapper = mapper;
  }

  @Override
  public String process(HttpWrapper wrapper) {
    String id = ConstraintRequests.requireIdOf(wrapper, "PUT");
    ConstraintUpdateRequest request =
        ConstraintRequests.readBody(wrapper, mapper, ConstraintUpdateRequest.class);
    String source = ConstraintRequests.requireField(request.source(), "source", mapper);
    // the virtual model locks itself while changing constraints, so the revision checked here
    // is the one the change is applied to
    synchronized (model) {
      ConstraintRequests.checkRevision(wrapper, model);
      ConstraintDescriptor updated;
      try {
        updated = model.updateConstraint(id, source);
      } catch (RuntimeException e) {
        throw ConstraintRequests.toError(e, mapper);
      }
      return ConstraintRequests.respond(wrapper, model, mapper, ConstraintDto.from(updated));
    }
  }
}
