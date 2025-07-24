package tools.vitruv.framework.remote.server;

import java.io.IOException;

import tools.vitruv.framework.remote.common.DefaultConnectionSettings;
import tools.vitruv.framework.vsum.VirtualModel;

/**
 * A Vitruvius server provides a REST-based API for Vitruvius. Therefore,
 * it takes a {@link VirtualModelInitializer} which is responsible to create an instance
 * of a {@link VirtualModel virtual model}. Once the server is started, the API can be used by the
 * Vitruvius client to perform remote actions on Vitruvius.
 */
public interface VitruviusServer {
    /**
     * Initializes a {@link VitruviusServer} using the given {@link VirtualModelInitializer}.
     * Sets host name or IP address and port which are used to open the server.
     *
     * @param modelInitializer The initializer which creates an {@link VirtualModel}.
     * @param port             The port to open to server on.
     * @param hostOrIp         The host name or IP address to which the server is bound.
     * @throws Exception if the initialization fails.
     */
    public void initialize(VirtualModelInitializer modelInitializer, int port, String hostOrIp) throws Exception;
    
    /**
     * Initializes a {@link VitruviusServer} using the given {@link VirtualModelInitializer}.
     * Sets the port which is used to open the server on to the given one.
     *
     * @param modelInitializer The initializer which creates an {@link VirtualModel}.
     * @param port             The port to open to server on.
     * @throws Exception if the initialization fails.
     */
    public default void initialize(VirtualModelInitializer modelInitializer, int port) throws Exception {
        initialize(modelInitializer, port, DefaultConnectionSettings.STD_HOST);
    }

    /**
     * Initializes a {@link VitruviusServer} using the given {@link VirtualModelInitializer}.
     * Sets the port which is used to open the server on to 8080.
     *
     * @param modelInitializer The initializer which creates an {@link tools.vitruv.framework.vsum.internal.InternalVirtualModel}.
     * @throws Exception if the initialization fails.
     */
    public default void initialize(VirtualModelInitializer modelInitializer) throws Exception {
        initialize(modelInitializer, DefaultConnectionSettings.STD_PORT);
    }

    /**
     * Returns the base URL of the server, usually in the form <code>http(s)://&lt;domain&gt;[:&lt;port&gt;]</code>.
     * 
     * @return the base URL.
     */
    public String getBaseUrl();

    /**
     * Starts the Vitruvius server.
     */
    public void start();

    /**
     * Stops the Vitruvius server.
     */
    public void stop();
}
