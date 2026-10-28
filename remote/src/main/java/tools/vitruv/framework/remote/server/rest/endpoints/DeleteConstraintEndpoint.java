package tools.vitruv.framework.remote.server.rest.endpoints;

import static java.net.HttpURLConnection.HTTP_NO_CONTENT;

import tools.vitruv.framework.remote.common.json.JsonMapper;
import tools.vitruv.framework.remote.server.http.HttpWrapper;
import tools.vitruv.framework.remote.server.rest.DeleteEndpoint;
import tools.vitruv.framework.vsum.VirtualModel;

/**
 * This endpoint deletes the constraint whose id is given in the path, including constraints the
 * virtual model was built with. An {@code If-Match} header restricts the deletion to the given
 * revision of the constraints.
 */
public class DeleteConstraintEndpoint implements DeleteEndpoint {
  private final VirtualModel model;
  private final JsonMapper mapper;

  /**
   * Creates a new DeleteConstraintEndpoint.
   *
   * @param model The virtual model whose constraints are deleted.
   * @param mapper The JSON mapper to use.
   */
  public DeleteConstraintEndpoint(VirtualModel model, JsonMapper mapper) {
    this.model = model;
    this.mapper = mapper;
  }

  @Override
  public String process(HttpWrapper wrapper) {
    String id = ConstraintRequests.requireIdOf(wrapper, "DELETE");
    synchronized (model) {
      ConstraintRequests.checkRevision(wrapper, model);
      try {
        model.deleteConstraint(id);
      } catch (RuntimeException e) {
        throw ConstraintRequests.toError(e, mapper);
      }
      wrapper.setStatusCode(HTTP_NO_CONTENT);
      return ConstraintRequests.respond(wrapper, model, mapper, null);
    }
  }
}
