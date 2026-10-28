package tools.vitruv.framework.remote.server.rest.endpoints;

import static java.net.HttpURLConnection.HTTP_BAD_METHOD;
import static java.net.HttpURLConnection.HTTP_CREATED;

import tools.vitruv.framework.remote.common.json.JsonMapper;
import tools.vitruv.framework.remote.common.rest.constants.EndpointPath;
import tools.vitruv.framework.remote.common.rest.constants.Header;
import tools.vitruv.framework.remote.common.rest.constraints.ConstraintCreateRequest;
import tools.vitruv.framework.remote.common.rest.constraints.ConstraintDto;
import tools.vitruv.framework.remote.server.exception.ServerHaltingException;
import tools.vitruv.framework.remote.server.http.HttpWrapper;
import tools.vitruv.framework.remote.server.rest.PostEndpoint;
import tools.vitruv.framework.vsum.VirtualModel;
import tools.vitruv.framework.vsum.constraints.ConstraintDescriptor;

/**
 * This endpoint creates a constraint at the virtual model. It answers with 201 and the created
 * constraint, whose path is given in the {@code Location} header.
 */
public class CreateConstraintEndpoint implements PostEndpoint {
  private final VirtualModel model;
  private final JsonMapper mapper;

  /**
   * Creates a new CreateConstraintEndpoint.
   *
   * @param model The virtual model to create the constraints at.
   * @param mapper The JSON mapper to use.
   */
  public CreateConstraintEndpoint(VirtualModel model, JsonMapper mapper) {
    this.model = model;
    this.mapper = mapper;
  }

  @Override
  public String process(HttpWrapper wrapper) {
    if (ConstraintRequests.idOf(wrapper).isPresent()) {
      throw new ServerHaltingException(
          HTTP_BAD_METHOD, "Constraints are created at the path /vsum/constraints");
    }
    ConstraintCreateRequest request =
        ConstraintRequests.readBody(wrapper, mapper, ConstraintCreateRequest.class);
    String language = ConstraintRequests.requireField(request.language(), "language", mapper);
    String source = ConstraintRequests.requireField(request.source(), "source", mapper);
    synchronized (model) {
      ConstraintDescriptor created;
      try {
        created = model.createConstraint(language, source);
      } catch (RuntimeException e) {
        throw ConstraintRequests.toError(e, mapper);
      }
      wrapper.setStatusCode(HTTP_CREATED);
      wrapper.addResponseHeader(Header.LOCATION, EndpointPath.constraint(created.id()));
      return ConstraintRequests.respond(wrapper, model, mapper, ConstraintDto.from(created));
    }
  }
}
