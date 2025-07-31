package tools.vitruv.remote.secserver.config;

/**
 * Defines the modes in which the security server can operate.
 */
public enum ServerModes {
    /**
     * The security server directly handles incoming requests with the existing Vitruvius server endpoint implementations.
     */
    DIRECT_CONNECTION,
    /**
     * The security server proxies incoming requests to a self-started Vitruvius server.
     */
    PROXY,
    /**
     * The security server acts as a reverse proxy for multiple Vitruvius server, which need to be registered.
     */
    REVERSE_PROXY;
}
