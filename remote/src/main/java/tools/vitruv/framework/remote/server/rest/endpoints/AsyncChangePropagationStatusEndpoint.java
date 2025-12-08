package tools.vitruv.framework.remote.server.rest.endpoints;

import tools.vitruv.framework.remote.common.json.JsonMapper;
import tools.vitruv.framework.remote.common.rest.constants.ContentType;
import tools.vitruv.framework.remote.common.rest.constants.Header;
import tools.vitruv.framework.remote.server.exception.ServerHaltingException;
import tools.vitruv.framework.remote.server.http.HttpWrapper;
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

		// TODO: Look up task status from registry
		// For now, just return placeholder status
		wrapper.setContentType(ContentType.TEXT_PLAIN);
		return "Still Working on it! Task ID: " + taskId;
	}

}
