package tools.vitruv.framework.remote.server;

import tools.vitruv.framework.remote.common.DefaultConnectionSettings;
import tools.vitruv.framework.remote.common.json.JsonMapper;
import tools.vitruv.framework.remote.server.http.java.VitruvJavaHttpServer;
import tools.vitruv.framework.remote.server.rest.endpoints.EndpointsProvider;

/**
 * A {@link VitruviusServer} implementation using clear-text HTTP/1.1.
 */
public class VitruvServer implements VitruviusServer {
    private boolean isInitialized = false;
    private VitruvServerConfiguration config;
    private VitruvJavaHttpServer server;
    private String baseUrl;

    /**
     * Creates a new {@link VitruvServer}.
     * Sets host name or IP address and port which are used to open the server.
     * Delegates to the appropriate constructor.
     *
     * @param modelInitializer Ignored.
     * @param port             The port to open to server on.
     * @param hostOrIp         The host name or IP address to which the server is bound.
     * @deprecated Here for backwards-compatibility. Please use constructur with the {@link VitruvServerConfiguration}
     *             parameter and the {@link initialize} method.
     */
    @Deprecated()
    public VitruvServer(VirtualModelInitializer modelInitializer, int port, String hostOrIp) {
    	this(new VitruvServerConfiguration(hostOrIp, port));
    }
    
    /**
     * Creates a new {@link VitruvServer}.
     * Sets the port which is used to open the server on to the given one.
     * Delegates to the appropriate constructor.
     *
     * @param modelInitializer Ignored.
     * @param port             The port to open to server on.
     * @deprecated Here for backwards-compatibility. Please use constructur with the {@link VitruvServerConfiguration}
     *             parameter and the {@link initialize} method.
     */
    @Deprecated
    public VitruvServer(VirtualModelInitializer modelInitializer, int port) {
    	this(modelInitializer, port, DefaultConnectionSettings.STD_HOST);
    }

    /**
     * Creates a new {@link VitruvServer}.
     * Sets the port which is used to open the server on to 8080.
     * Delegates to the appropriate constructor.
     *
     * @param modelInitializer Ignored.
     * @deprecated Here for backwards-compatibility. Please use constructur with the {@link VitruvServerConfiguration}
     *             parameter and the {@link initialize} method.
     */
    @Deprecated
    public VitruvServer(VirtualModelInitializer modelInitializer) {
        this(modelInitializer, DefaultConnectionSettings.STD_PORT);
    }

    /**
     * Creates a new {@link VitruvServer}.
     * 
     * @param config Configuration for the server.
     */
    public VitruvServer(VitruvServerConfiguration config) {
        this.config = config;
    }

    @Override
    public void initialize(VirtualModelInitializer modelInitializer) throws Exception {
        if (this.isInitialized) {
            return;
        }

        var model = modelInitializer.init();
        var mapper = new JsonMapper(model.getFolder());
        var endpoints = EndpointsProvider.getAllEndpoints(model, mapper);

        this.server = new VitruvJavaHttpServer(this.config.hostOrIp(), this.config.port(), endpoints);
        this.baseUrl = "http://" + this.config.hostOrIp() + ":" + this.config.port();
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
