package tools.vitruv.framework.remote.common;

/**
 * This enum lists all available HTTP versions, The support for the different versions
 * depends on the concrete server and client implementations.
 */
public enum AvailableHttpVersions {
    /**
     * Represents HTTP/1.1, clear-text and secured (HTTPS).
     */
    HTTP_1_1,
    /**
     * Represents HTTP/2, clear-text and secured (HTTPS).
     */
    HTTP_2,
    /**
     * Represents HTTP/3, only secured (HTTPS) over QUIC (over UDP).
     */
    HTTP_3
}
