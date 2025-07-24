package tools.vitruv.framework.remote.server;

import tools.vitruv.framework.remote.common.json.JsonMapper;
import tools.vitruv.framework.remote.server.http.java.VitruvJavaHttpServer;
import tools.vitruv.framework.remote.server.rest.endpoints.EndpointsProvider;
import tools.vitruv.framework.vsum.VirtualModel;

/**
 * A {@link VitruviusServer} implementation using unsecured HTTP/1.1.
 */
public class VitruvServer implements VitruviusServer {
    private boolean isInitialized = false;
    private VitruvJavaHttpServer server;
    private String baseUrl;

    /**
     * Creates a new {@link VitruvServer} using the given {@link VirtualModelInitializer}.
     * Sets host name or IP address and port which are used to open the server.
     * Delegates to the appropriate {@link initialize} method.
     *
     * @param modelInitializer The initializer which creates an {@link VirtualModel}.
     * @param port             The port to open to server on.
     * @param hostOrIp         The host name or IP address to which the server is bound.
     * @deprecated Here for backwards-compatibility. Please use default constructur and an {@link initialize} method.
     */
    @Deprecated()
    public VitruvServer(VirtualModelInitializer modelInitializer, int port, String hostOrIp) throws Exception {
    	initialize(modelInitializer, port, hostOrIp);
    }
    
    /**
     * Creates a new {@link VitruvServer} using the given {@link VirtualModelInitializer}.
     * Sets the port which is used to open the server on to the given one.
     * Delegates to the appropriate {@link initialize} method.
     *
     * @param modelInitializer The initializer which creates an {@link VirtualModel}.
     * @param port             The port to open to server on.
     * @deprecated Here for backwards-compatibility. Please use default constructur and an {@link initialize} method.
     */
    @Deprecated
    public VitruvServer(VirtualModelInitializer modelInitializer, int port) throws Exception {
    	initialize(modelInitializer, port);
    }

    /**
     * Creates a new {@link VitruvServer} using the given {@link VirtualModelInitializer}.
     * Sets the port which is used to open the server on to 8080.
     * Delegates to the appropriate {@link initialize} method.
     *
     * @param modelInitializer The initializer which creates an {@link tools.vitruv.framework.vsum.internal.InternalVirtualModel}.
     * @deprecated Here for backwards-compatibility. Please use default constructur and an {@link initialize} method.
     */
    @Deprecated
    public VitruvServer(VirtualModelInitializer modelInitializer) throws Exception {
        initialize(modelInitializer);
    }

    /**
     * Creates a new {@link VitruvServer}.
     */
    public VitruvServer() {}

    @Override
    public void initialize(VirtualModelInitializer modelInitializer, int port, String hostOrIp) throws Exception {
        if (this.isInitialized) {
            return;
        }

        var model = modelInitializer.init();
        var mapper = new JsonMapper(model.getFolder());
        var endpoints = EndpointsProvider.getAllEndpoints(model, mapper);

        this.server = new VitruvJavaHttpServer(hostOrIp, port, endpoints);
        this.baseUrl = "http://" + hostOrIp + ":" + port;
        this.isInitialized = true;
    }

    @Override
    public String getBaseUrl() {
        return this.baseUrl;
    }

    @Override
    public void start() {
        if (!this.isInitialized) {
            throw new IllegalStateException("Server not initialized.");
        }

        server.start();
    }

    @Override
    public void stop() {
        if (!this.isInitialized) {
            throw new IllegalStateException("Server not initialized.");
        }

        server.stop();
    }
}
