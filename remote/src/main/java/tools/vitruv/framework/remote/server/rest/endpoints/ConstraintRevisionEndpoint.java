package tools.vitruv.framework.remote.server.rest.endpoints;

import tools.vitruv.framework.remote.common.json.JsonMapper;
import tools.vitruv.framework.remote.common.rest.constraints.ConstraintRevisionDto;
import tools.vitruv.framework.remote.server.http.HttpWrapper;
import tools.vitruv.framework.remote.server.rest.GetEndpoint;
import tools.vitruv.framework.vsum.VirtualModel;

/**
 * This endpoint returns the revision of the constraints of the virtual model, so clients can
 * cheaply detect whether the constraints changed.
 */
public class ConstraintRevisionEndpoint implements GetEndpoint {
  private final VirtualModel model;
  private final JsonMapper mapper;

  /**
   * Creates a new ConstraintRevisionEndpoint.
   *
   * @param model The virtual model providing the constraints.
   * @param mapper The JSON mapper to serialize the response.
   */
  public ConstraintRevisionEndpoint(VirtualModel model, JsonMapper mapper) {
    this.model = model;
    this.mapper = mapper;
  }

  @Override
  public String process(HttpWrapper wrapper) {
    // the body and the ETag header must show the same revision
    synchronized (model) {
      String revision = ConstraintRequests.revisionOf(model);
      return ConstraintRequests.respond(
          wrapper, model, mapper, new ConstraintRevisionDto(revision));
    }
  }
}
