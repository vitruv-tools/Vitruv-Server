package tools.vitruv.framework.remote.server.rest.endpoints;

import tools.vitruv.framework.remote.common.json.JsonMapper;
import tools.vitruv.framework.remote.common.rest.constants.ContentType;
import tools.vitruv.framework.remote.common.rest.constants.Header;
import tools.vitruv.framework.remote.server.exception.ServerHaltingException;
import tools.vitruv.framework.remote.server.http.HttpWrapper;
import tools.vitruv.framework.remote.server.registry.AsyncTaskRegistry;
import tools.vitruv.framework.remote.server.registry.AsyncTaskStatus;
import tools.vitruv.framework.remote.server.rest.GetEndpoint;

public class AsyncChangePropagationStatusEndpoint implements GetEndpoint {
	private final JsonMapper mapper;

	public AsyncChangePropagationStatusEndpoint(JsonMapper mapper) {
		this.mapper = mapper;
	}

	@SuppressWarnings("unchecked")
	@Override
	public String process(HttpWrapper wrapper) throws ServerHaltingException {
		String taskId = wrapper.getRequestHeader(Header.TASK_ID);

		if (taskId == null || taskId.isEmpty()) {
			throw notFound("Task ID query parameter is missing!");
		}

		AsyncTaskStatus status = AsyncTaskRegistry.getInstance().getStatus(taskId);
		if (status == null) {
			throw notFound("Task with ID '" + taskId + "' not found!");
		}

		wrapper.setContentType(ContentType.TEXT_PLAIN);
		return "Task State: " + status.getState() + " | Created: " + status.getCreatedAt();
	}

}
