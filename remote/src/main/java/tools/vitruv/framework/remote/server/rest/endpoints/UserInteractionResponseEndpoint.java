package tools.vitruv.framework.remote.server.rest.endpoints;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;

import tools.vitruv.change.interaction.UserInteractionBase;
import tools.vitruv.framework.remote.common.json.JsonMapper;
import tools.vitruv.framework.remote.common.rest.constants.ContentType;
import tools.vitruv.framework.remote.common.rest.constants.Header;
import tools.vitruv.framework.remote.server.exception.ServerHaltingException;
import tools.vitruv.framework.remote.server.http.HttpWrapper;
import tools.vitruv.framework.remote.server.registry.AsyncTaskRegistry;
import tools.vitruv.framework.remote.server.registry.AsyncTaskStatus;
import tools.vitruv.framework.remote.server.rest.PostEndpoint;

public class UserInteractionResponseEndpoint implements PostEndpoint {
	private final JsonMapper mapper;

	public UserInteractionResponseEndpoint(JsonMapper mapper) {
		this.mapper = mapper;
	}

	@SuppressWarnings("unchecked")
	@Override
	public String process(HttpWrapper wrapper) throws ServerHaltingException {
		String taskId = wrapper.getRequestHeader(Header.TASK_ID);
		if (taskId == null || taskId.isEmpty()) {
			throw notFound("Task ID query parameter is missing!");
		}

		AsyncTaskStatus status = AsyncTaskRegistry.getInstance().getTaskStatus(taskId);
		if (status == null) {
			throw notFound("Task with ID '" + taskId + "' not found!");
		}

		if (status.getState() != AsyncTaskStatus.TaskState.WAITING_USER_INTERACTION) {
			throw new ServerHaltingException(409, "Task with ID '" + taskId + "' is not waiting for user interaction!");
		}

		String body;
		try {
			body = wrapper.getRequestBodyAsString();
		} catch (IOException e) {
			throw internalServerError(e.getMessage());
		}

		UserInteractionBase response;
		try {
			response = mapper.deserialize(body, UserInteractionBase.class);
		} catch (JsonProcessingException e) {
			throw internalServerError(e.getMessage());
		}

		status.setInteractionResponse(response);
		wrapper.setContentType(ContentType.TEXT_PLAIN);
		return "OK";
	}

}
