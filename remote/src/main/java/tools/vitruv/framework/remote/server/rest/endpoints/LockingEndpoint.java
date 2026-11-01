package tools.vitruv.framework.remote.server.rest.endpoints;

import tools.vitruv.framework.remote.server.http.HttpWrapper;
import tools.vitruv.framework.remote.server.rest.DeleteEndpoint;
import tools.vitruv.framework.remote.server.rest.GetEndpoint;
import tools.vitruv.framework.remote.server.rest.PatchEndpoint;
import tools.vitruv.framework.remote.server.rest.PostEndpoint;
import tools.vitruv.framework.remote.server.rest.PutEndpoint;
import tools.vitruv.framework.remote.server.rest.RestEndpoint;

/**
 * Runs another endpoint while holding the VSUM lock of the server, so the endpoint never works on
 * the VSUM or its views at the same time as the propagation worker. It can stand in for an endpoint
 * of any HTTP method.
 */
class LockingEndpoint
    implements GetEndpoint, PostEndpoint, PutEndpoint, PatchEndpoint, DeleteEndpoint {
  private final Object vsumLock;
  private final RestEndpoint endpoint;

  /**
   * Creates a new LockingEndpoint.
   *
   * @param vsumLock the lock to hold
   * @param endpoint the endpoint to run while holding it
   */
  LockingEndpoint(Object vsumLock, RestEndpoint endpoint) {
    this.vsumLock = vsumLock;
    this.endpoint = endpoint;
  }

  @Override
  public String process(HttpWrapper wrapper) {
    synchronized (vsumLock) {
      return endpoint.process(wrapper);
    }
  }
}
