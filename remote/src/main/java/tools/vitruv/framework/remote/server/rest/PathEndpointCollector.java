package tools.vitruv.framework.remote.server.rest;

public record PathEndpointCollector(
	String path,
	GetEndpoint getEndpoint,
	PostEndpoint postEndpoint,
	PutEndpoint putEndpoint,
	PatchEndpoint patchEndpoint,
	DeleteEndpoint deleteEndpoint) {}
