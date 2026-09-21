package tools.vitruv.framework.remote.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.vitruv-server")
public record VitruvServerProperties(
        String url
) {
}
