package tools.vitruv.framework.remote.server.rest;

/**
 * Maps requests under the given <code>path</code> to the appropriate {@link RestEndpoint}.
 *
 * @param getEndpoint - {@link GetEndpoint}
 * @param postEndpoint - {@link PostEndpoint}
 * @param putEndpoint - {@link PutEndpoint}
 * @param patchEndpoint - {@link PatchEndpoint}
 * @param deleteEndpoint - {@link DeleteEndpoint}
 */
public record PathEndpointCollector(
    String path,
    GetEndpoint getEndpoint,
    PostEndpoint postEndpoint,
    PutEndpoint putEndpoint,
    PatchEndpoint patchEndpoint,
    DeleteEndpoint deleteEndpoint) {}
