package tools.vitruv.framework.remote.server.rest.endpoints;

import tools.vitruv.framework.remote.common.json.JsonMapper;
import tools.vitruv.framework.remote.common.rest.TaskStatus;
import tools.vitruv.framework.remote.server.http.HttpWrapper;
import tools.vitruv.framework.remote.server.propagation.PropagationWorker;
import tools.vitruv.framework.remote.server.rest.GetEndpoint;

/**
 * This endpoint returns the {@link TaskStatus} of a change that is propagated asynchronously. It
 * answers right away, also while a change is being propagated.
 */
public class TaskStatusEndpoint implements GetEndpoint {
  private final PropagationWorker propagationWorker;
  private final JsonMapper mapper;

  /**
   * Creates a new TaskStatusEndpoint.
   *
   * @param propagationWorker The worker that propagates the changes.
   * @param mapper The JSON mapper to use.
   */
  public TaskStatusEndpoint(PropagationWorker propagationWorker, JsonMapper mapper) {
    this.propagationWorker = propagationWorker;
    this.mapper = mapper;
  }

  @Override
  public String process(HttpWrapper wrapper) {
    int taskId = TaskEndpointSupport.readTaskId(wrapper);
    TaskStatus status =
        propagationWorker
            .getStatus(taskId)
            .orElseThrow(() -> TaskEndpointSupport.unknownTask(taskId));
    return TaskEndpointSupport.respondWith(status, wrapper, mapper);
  }
}
