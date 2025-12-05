package tools.vitruv.framework.remote.server.rest.endpoints;

import tools.vitruv.framework.remote.common.json.JsonMapper;
import tools.vitruv.framework.remote.server.exception.ServerHaltingException;
import tools.vitruv.framework.remote.server.http.HttpWrapper;
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
		// TODO Auto-generated method stub
		return null;
	}

}
