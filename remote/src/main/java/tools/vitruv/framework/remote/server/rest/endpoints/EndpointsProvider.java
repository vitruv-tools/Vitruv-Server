package tools.vitruv.framework.remote.server.rest.endpoints;

import java.util.ArrayList;
import java.util.List;
import tools.vitruv.framework.remote.common.json.JsonMapper;
import tools.vitruv.framework.remote.common.rest.constants.EndpointPath;
import tools.vitruv.framework.remote.server.exception.ServerHaltingException;
import tools.vitruv.framework.remote.server.http.HttpWrapper;
import tools.vitruv.framework.remote.server.propagation.PropagationWorker;
import tools.vitruv.framework.remote.server.rest.DeleteEndpoint;
import tools.vitruv.framework.remote.server.rest.GetEndpoint;
import tools.vitruv.framework.remote.server.rest.PatchEndpoint;
import tools.vitruv.framework.remote.server.rest.PathEndointCollector;
import tools.vitruv.framework.remote.server.rest.PostEndpoint;
import tools.vitruv.framework.remote.server.rest.PutEndpoint;
import tools.vitruv.framework.vsum.VirtualModel;

/** Provides all REST endpoints for the Vitruv server. */
public class EndpointsProvider {

  /**
   * Creates and returns all REST endpoints for the Vitruv server.
   *
   * <p>Every endpoint that works on the VSUM or its views holds the given lock while it runs, so it
   * never overlaps with a step of the propagation worker. The health check and the endpoints for
   * asynchronously propagated changes do not, so they answer while a change is being propagated.
   *
   * @param virtualModel The virtual model to use.
   * @param mapper The JSON mapper to use.
   * @param vsumLock The lock held by everyone who works on the VSUM or its views.
   * @param propagationWorker The worker that propagates changes asynchronously.
   * @return A list of all REST endpoints.
   */
  public static List<PathEndointCollector> getAllEndpoints(
      VirtualModel virtualModel,
      JsonMapper mapper,
      Object vsumLock,
      PropagationWorker propagationWorker) {
    var defaultEndpoints = getDefaultEndpoints();

    List<PathEndointCollector> result = new ArrayList<>();
    result.add(
        new PathEndointCollector(
            EndpointPath.HEALTH,
            new HealthEndpoint(),
            defaultEndpoints.postEndpoint(),
            defaultEndpoints.putEndpoint(),
            defaultEndpoints.patchEndpoint(),
            defaultEndpoints.deleteEndpoint()));
    result.add(
        new PathEndointCollector(
            EndpointPath.IS_VIEW_CLOSED,
            new LockingEndpoint(vsumLock, new IsViewClosedEndpoint()),
            defaultEndpoints.postEndpoint(),
            defaultEndpoints.putEndpoint(),
            defaultEndpoints.patchEndpoint(),
            defaultEndpoints.deleteEndpoint()));
    result.add(
        new PathEndointCollector(
            EndpointPath.IS_VIEW_OUTDATED,
            new LockingEndpoint(vsumLock, new IsViewOutdatedEndpoint()),
            defaultEndpoints.postEndpoint(),
            defaultEndpoints.putEndpoint(),
            defaultEndpoints.patchEndpoint(),
            defaultEndpoints.deleteEndpoint()));
    result.add(
        new PathEndointCollector(
            EndpointPath.VIEW,
            new LockingEndpoint(vsumLock, new UpdateViewEndpoint(mapper)),
            new LockingEndpoint(vsumLock, new ViewEndpoint(mapper)),
            defaultEndpoints.putEndpoint(),
            // Takes the lock itself, only when it propagates the changes before answering.
            new ChangePropagationEndpoint(mapper, vsumLock, propagationWorker),
            new LockingEndpoint(vsumLock, new CloseViewEndpoint())));
    result.add(
        new PathEndointCollector(
            EndpointPath.VIEW_SELECTOR,
            new LockingEndpoint(vsumLock, new ViewSelectorEndpoint(virtualModel, mapper)),
            defaultEndpoints.postEndpoint(),
            defaultEndpoints.putEndpoint(),
            defaultEndpoints.patchEndpoint(),
            defaultEndpoints.deleteEndpoint()));
    result.add(
        new PathEndointCollector(
            EndpointPath.VIEW_TYPES,
            new LockingEndpoint(vsumLock, new ViewTypesEndpoint(virtualModel, mapper)),
            defaultEndpoints.postEndpoint(),
            defaultEndpoints.putEndpoint(),
            defaultEndpoints.patchEndpoint(),
            defaultEndpoints.deleteEndpoint()));
    result.add(
        new PathEndointCollector(
            EndpointPath.CHANGE_DERIVING,
            defaultEndpoints.getEndpoint(),
            defaultEndpoints.postEndpoint(),
            defaultEndpoints.putEndpoint(),
            new LockingEndpoint(vsumLock, new ChangeDerivingEndpoint(mapper)),
            defaultEndpoints.deleteEndpoint()));
    result.add(
        new PathEndointCollector(
            EndpointPath.TASK,
            new TaskStatusEndpoint(propagationWorker, mapper),
            new UserInteractionEndpoint(propagationWorker, mapper),
            defaultEndpoints.putEndpoint(),
            defaultEndpoints.patchEndpoint(),
            defaultEndpoints.deleteEndpoint()));

    return result;
  }

  private static PathEndointCollector getDefaultEndpoints() {
    var getEndpoint =
        new GetEndpoint() {
          @Override
          public String process(HttpWrapper wrapper) throws ServerHaltingException {
            throw notFound("Get mapping for this request path not found!");
          }
        };
    var postEndpoint =
        new PostEndpoint() {
          @Override
          public String process(HttpWrapper wrapper) throws ServerHaltingException {
            throw notFound("Post mapping for this request path not found!");
          }
        };
    var patchEndpoint =
        new PatchEndpoint() {
          @Override
          public String process(HttpWrapper wrapper) throws ServerHaltingException {
            throw notFound("Patch mapping for this request path not found!");
          }
        };
    var deleteEndpoint =
        new DeleteEndpoint() {
          @Override
          public String process(HttpWrapper wrapper) throws ServerHaltingException {
            throw notFound("Delete mapping for this request path not found!");
          }
        };
    var putEndpoint =
        new PutEndpoint() {
          @Override
          public String process(HttpWrapper wrapper) throws ServerHaltingException {
            throw notFound("Put mapping for this request path not found!");
          }
        };
    return new PathEndointCollector(
        "", getEndpoint, postEndpoint, putEndpoint, patchEndpoint, deleteEndpoint);
  }

  private EndpointsProvider() {}
}
