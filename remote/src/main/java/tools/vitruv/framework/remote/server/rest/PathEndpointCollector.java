package tools.vitruv.framework.remote.server.rest;

/**
 * Maps requests under the given <code>path</code> to the appropriate {@link RestEndpoint}.
 */
public record PathEndpointCollector(
    String path,
    GetEndpoint getEndpoint,
    PostEndpoint postEndpoint,
    PutEndpoint putEndpoint,
    PatchEndpoint patchEndpoint,
    DeleteEndpoint deleteEndpoint) {}
