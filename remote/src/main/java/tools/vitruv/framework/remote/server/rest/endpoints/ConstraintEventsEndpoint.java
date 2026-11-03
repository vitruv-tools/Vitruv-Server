package tools.vitruv.framework.remote.server.rest.endpoints;

import java.io.IOException;
import tools.vitruv.framework.remote.server.exception.ServerHaltingException;
import tools.vitruv.framework.remote.server.http.HttpWrapper;
import tools.vitruv.framework.remote.server.rest.GetEndpoint;

/**
 * This endpoint subscribes the client to the results of constraint validations, sent as
 * Server-Sent Events by a {@link ConstraintEventBroadcaster}. The response stays open until the
 * client disconnects or the server stops.
 */
public class ConstraintEventsEndpoint implements GetEndpoint {
  private final ConstraintEventBroadcaster broadcaster;

  /**
   * Creates a new ConstraintEventsEndpoint.
   *
   * @param broadcaster The broadcaster sending the events.
   */
  public ConstraintEventsEndpoint(ConstraintEventBroadcaster broadcaster) {
    this.broadcaster = broadcaster;
  }

  @Override
  public String process(HttpWrapper wrapper) throws ServerHaltingException {
    try {
      broadcaster.subscribe(wrapper.openEventStream());
    } catch (IOException e) {
      throw internalServerError("Could not open the event stream: " + e.getMessage());
    }
    return null;
  }
}
