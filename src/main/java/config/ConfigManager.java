package config;

import java.io.InputStream;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigManager {
    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);
    private final Properties properties = new Properties();

    public ConfigManager(String configFileName) throws Exception {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(configFileName)) {
            if (input == null) {
                logger.error("Config file not found: " + configFileName);
                throw new Exception("Config file not found: " + configFileName); // TODO: exception not necessary
            }
            properties.load(input);
        }
    }

    public int getVitruvServerPort() {
        return Integer.parseInt(properties.getProperty("vitruv-server.port", "8080"));
    }

    public int getHttpsServerPort() {
        return Integer.parseInt(properties.getProperty("https-server.port", "8443"));
    }

    public String getClientId() {
        return properties.getProperty("client.id");
    }

    public String getClientSecret() {
        return properties.getProperty("client.secret");
    }

    public String getDomainProtocol() {
        return properties.getProperty("domain.protocol");
    }

    public String getDomainName() {
        return properties.getProperty("domain.name", "localhost:" + getHttpsServerPort());
    }
}