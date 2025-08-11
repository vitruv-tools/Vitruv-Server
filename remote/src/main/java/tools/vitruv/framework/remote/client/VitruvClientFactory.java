package tools.vitruv.framework.remote.client;

import tools.vitruv.framework.remote.client.http.JavaHttpClientWrapper;
import tools.vitruv.framework.remote.client.http.VitruvHttpClientWrapper;
import tools.vitruv.framework.remote.client.impl.VitruvRemoteConnection;
import tools.vitruv.framework.remote.common.DefaultConnectionSettings;

import java.nio.file.Path;

public class VitruvClientFactory {

    /**
     * Private constructor to prevent instantiation of this utility class.
     * 
     * @throws UnsupportedOperationException if an appempt is made to instantiate this class.
     */
    private VitruvClientFactory() {
        throw new UnsupportedOperationException("VitruvClientFactory is a utility class and cannot be instantiated.");
    }

	 /**
     * Creates a new {@link VitruvClient} using the Java HTTP client, the given host name or IP address,
     * and the standard port of 8080.
     *
     * @param hostOrIp The host name or IP address of the Vitruvius server.
     * @param temp A non-existing or empty directory for temporary files.
     * @return A {@link VitruvClient}.
     */
    public static VitruvClient create(String hostOrIp, Path temp) {
        return create(hostOrIp, DefaultConnectionSettings.STD_PORT, temp);
    }

    /**
     * Creates a new {@link VitruvClient} using the Java HTTP client, the given host name or IP address,
     * and port.
     *
     * @param hostOrIp The host name or IP address of the Vitruvius server.
     * @param port Port of the Vitruvius server.
     * @param temp A non-existing or empty directory for temporary files.
     * @return A {@link VitruvClient}.
     */
    public static VitruvClient create(String hostOrIp, int port, Path temp) {
        return create(DefaultConnectionSettings.STD_PROTOCOL, hostOrIp, port, temp);
    }
    
    /**
     * Creates a new {@link VitruvClient} using the Java HTTP client, the given protocol,
     * host name or IP address, and port.
     * 
     * @param protocol The protocol.
     * @param hostOrIp The host name of IP address of the Vitruvius server.
     * @param port Port of the Vitruvius server.
     * @param temp A non-existing or empty directory for temporary files.
     * @return A {@link VitruvClient}.
     */
    public static VitruvClient create(String protocol, String hostOrIp, int port, Path temp) {
    	return create(String.format("%s://%s:%d", protocol, hostOrIp, port), new JavaHttpClientWrapper(), temp);
    }

    /**
     * Creates a new {@link VitruvClient} using the given base URI and HTTP client wrapper.
     * 
     * @param uri The base URI of the Vitruvius server, against which requests are sent.
     * @param client A wrapper for an HTTP client, which handles the actual sending and receiving of HTTP requests / responses.
     * @param temp A non-existing or empty directory for temporary files.
     * @return A {@link VitruvClient}.
     */
    public static VitruvClient create(String uri, VitruvHttpClientWrapper client, Path temp) {
        return new VitruvRemoteConnection(uri, client, temp);
    }
}
