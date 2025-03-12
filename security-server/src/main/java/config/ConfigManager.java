package config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Properties;

public class ConfigManager {
    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);
    private final Properties properties = new Properties();

    private final static String CONFIG_FILE_NAME = "config.properties";

    public ConfigManager() throws Exception {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE_NAME)) {
            if (input == null) {
                logger.error("Config file not found: " + CONFIG_FILE_NAME);
                throw new Exception("Config file not found: " + CONFIG_FILE_NAME);
            }
            properties.load(input);
        }
    }

    public int getVitruvServerPort() {
        return Integer.parseInt(properties.getProperty("vitruv-server.port"));
    }

    public int getHttpsServerPort() {
        return Integer.parseInt(properties.getProperty("https-server.port"));
    }

    public String getDomainProtocol() {
        return properties.getProperty("domain.protocol");
    }

    public String getDomainName() {
        return properties.getProperty("domain.name");
    }

    public String getCertChainPath() {
        return properties.getProperty("cert.chain.path");
    }

    public String getCertKeyPath() {
        return properties.getProperty("cert.key.path");
    }

    public String getOidcClientId() {
        return System.getenv("OIDC_CLIENT_ID");
    }

    public String getOidcClientSecret() {
        return System.getenv("OIDC_CLIENT_SECRET");
    }

    public String getTlsPassword() {
        return System.getenv("TLS_PASSWORD");
    }
}