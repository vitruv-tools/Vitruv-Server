package tools.vitruv.framework.remote.server;

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
     * Configuration of connection properties is implementation-dependent.
     *
     * @param modelInitializer The initializer which creates an {@link VirtualModel}.
     * @throws Exception if the initialization fails.
     */
    public void initialize(VirtualModelInitializer modelInitializer) throws Exception;

    /**
     * Returns the base URL of the server, usually in the form <code>http(s)://&lt;domain&gt;[:&lt;port&gt;]</code>.
     * 
     * @return the base URL.
     */
    public String getBaseUrl();

    /**
     * Starts the Vitruvius server.
     * 
     * @throws Exception if the starting of the server fails.
     */
    public void start() throws Exception;

    /**
     * Stops the Vitruvius server.
     * 
     * @throws Exception if stopping the server fails.
     */
    public void stop() throws Exception;
}
