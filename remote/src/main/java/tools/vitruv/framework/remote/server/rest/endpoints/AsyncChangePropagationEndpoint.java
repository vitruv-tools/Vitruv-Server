package tools.vitruv.framework.remote.server.rest.endpoints;

import java.io.IOException;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import java.util.UUID;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;

import com.fasterxml.jackson.core.JsonProcessingException;

import tools.vitruv.change.atomic.root.InsertRootEObject;
import tools.vitruv.change.composite.description.VitruviusChange;
import tools.vitruv.framework.remote.common.json.JsonMapper;
import tools.vitruv.framework.remote.common.rest.constants.ContentType;
import tools.vitruv.framework.remote.common.rest.constants.Header;
import tools.vitruv.framework.remote.server.exception.ServerHaltingException;
import tools.vitruv.framework.remote.server.http.HttpWrapper;
import tools.vitruv.framework.remote.server.registry.AsyncTaskRegistry;
import tools.vitruv.framework.remote.server.rest.PostEndpoint;

public class AsyncChangePropagationEndpoint implements PostEndpoint {
	private static final String ENDPOINT_METRIC_NAME = "vitruv.server.rest.async.propagation";
	private final JsonMapper mapper;

	public AsyncChangePropagationEndpoint(JsonMapper mapper) {
		this.mapper = mapper;
	}

	@SuppressWarnings("unchecked")
	@Override
	public String process(HttpWrapper wrapper) throws ServerHaltingException {
		var viewUuid = wrapper.getRequestHeader(Header.VIEW_UUID);
		var view = Cache.getView(viewUuid);
		if (view == null) {
			throw notFound("View with given id not found!");
		}

		String body;
		try {
			body = wrapper.getRequestBodyAsString();
		} catch (IOException e) {
			throw internalServerError(e.getMessage());
		}

		@SuppressWarnings("rawtypes")
		VitruviusChange change;
		try {
			change = mapper.deserialize(body, VitruviusChange.class);
		} catch (JsonProcessingException e) {
			throw new ServerHaltingException(HTTP_BAD_REQUEST, e.getMessage());
		}

		change.getEChanges().forEach(it -> {
			if (it instanceof InsertRootEObject<?> echange) {
				echange.setResource(new ResourceImpl(URI.createURI(echange.getUri())));
			}
		});

		// Generate random task ID
		String taskId = UUID.randomUUID().toString();
		AsyncTaskRegistry.getInstance().registerTask(taskId);

		wrapper.setContentType(ContentType.TEXT_PLAIN);
		return taskId;
	}

}
