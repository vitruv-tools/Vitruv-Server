package tools.vitruv.framework.remote.server.rest.endpoints;

import tools.vitruv.framework.remote.common.json.JsonMapper;
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
		// TODO Auto-generated method stub
		return null;
	}

}
