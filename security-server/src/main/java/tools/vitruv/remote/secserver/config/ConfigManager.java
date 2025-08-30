package tools.vitruv.remote.secserver.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Manages configuration properties loaded from 'config.properties'.
 */
public class ConfigManager {
    public static final String KEY_VITRUVIUS_SERVER_PORT = "vitruv-server.port";
    public static final String KEY_HTTPS_SERVER_PORT = "https-server.port";
    public static final String KEY_DOMAIN_PROTOCOL = "domain.protocol";
    public static final String KEY_DOMAIN_NAME = "domain.name";
    public static final String KEY_CERT_CHAIN_PATH = "cert.chain.path";
    public static final String KEY_CERT_KEY_PATH = "cert.key.path";
    public static final String KEY_OIDC_CLIENT_ID = "OIDC_CLIENT_ID";
    public static final String KEY_OIDC_CLIENT_SECRET = "OIDC_CLIENT_SECRET";
    public static final String KEY_TLS_PASSWORD = "TLS_PASSWORD";
    public static final String KEY_OIDC_DISCOVERY_URI = "VITRUV_SERVER_OIDC_DISCOVERY_URI";
    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);
    private final Properties properties = new Properties();
    private final static String CONFIG_FILE_NAME = "config.properties";

    /**
     * Loads configuration properties from 'config.properties'.
     *
     * @throws IOException if the config file is missing or cannot be loaded
     */
    public void initialize() throws IOException {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE_NAME)) {
            if (input == null) {
                logger.error("Config file not found: " + CONFIG_FILE_NAME);
                throw new IOException("Config file not found: " + CONFIG_FILE_NAME);
            }
            properties.load(input);
        }
    }

    public int getVitruvServerPort() {
        return Integer.parseInt(properties.getProperty(KEY_VITRUVIUS_SERVER_PORT));
    }

    public int getHttpsServerPort() {
        return Integer.parseInt(properties.getProperty(KEY_HTTPS_SERVER_PORT));
    }

    public String getDomainProtocol() {
        return properties.getProperty(KEY_DOMAIN_PROTOCOL);
    }

    public String getDomainName() {
        return properties.getProperty(KEY_DOMAIN_NAME);
    }

    public String getCertChainPath() {
        return properties.getProperty(KEY_CERT_CHAIN_PATH);
    }

    public String getCertKeyPath() {
        return properties.getProperty(KEY_CERT_KEY_PATH);
    }

    public String getOidcClientId() {
        return properties.getProperty(KEY_OIDC_CLIENT_ID);
    }

    public String getOidcClientSecret() {
        return properties.getProperty(KEY_OIDC_CLIENT_SECRET);
    }

    public String getTlsPassword() {
        return properties.getProperty(KEY_TLS_PASSWORD);
    }

    public String getOIDCDiscoveryUri() {
        return properties.getProperty(KEY_OIDC_DISCOVERY_URI);
    }
}