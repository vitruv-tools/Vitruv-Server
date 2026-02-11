package tools.vitruv.framework.remote.server.rest.endpoints;

import com.fasterxml.jackson.core.JsonProcessingException;

import tools.vitruv.change.interaction.UserInteractionBase;
import tools.vitruv.framework.remote.common.json.JsonMapper;
import tools.vitruv.framework.remote.common.rest.constants.ContentType;
import tools.vitruv.framework.remote.common.rest.constants.Header;
import tools.vitruv.framework.remote.server.exception.ServerHaltingException;
import tools.vitruv.framework.remote.server.http.HttpWrapper;
import tools.vitruv.framework.remote.server.registry.AsyncTaskRegistry;
import tools.vitruv.framework.remote.server.registry.AsyncTaskStatus;
import tools.vitruv.framework.remote.server.rest.GetEndpoint;

public class GetUserInteractionEndpoint implements GetEndpoint {
	private final JsonMapper mapper;

	public GetUserInteractionEndpoint(JsonMapper mapper) {
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

		wrapper.setContentType(ContentType.APPLICATION_JSON);
		if (!status.isWaitingForUserInteraction()) {
			return "null";
		}

		UserInteractionBase interaction = status.getPendingInteraction();
		if (interaction == null) {
			return "null";
		}

		try {
			return mapper.serialize(interaction);
		} catch (JsonProcessingException e) {
			throw internalServerError(e.getMessage());
		}
	}

}
