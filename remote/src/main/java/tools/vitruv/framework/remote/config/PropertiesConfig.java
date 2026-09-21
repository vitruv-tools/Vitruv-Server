package tools.vitruv.framework.remote.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for registering application properties with the Spring context.
 * <p>
 * This class enables the binding of external configuration properties to the respective
 * configuration records.
 * <p>
 * By using the {@code @EnableConfigurationProperties} annotation, this class ensures that
 * the specified properties classes will be processed and available as beans in the application
 * context, allowing them to be injected where needed.
 */
@Configuration
@EnableConfigurationProperties({VsumProperties.class, VitruvServerProperties.class, PropagationProperties.class})
public class PropertiesConfig {
}
