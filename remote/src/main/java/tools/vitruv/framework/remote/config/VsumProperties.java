package tools.vitruv.framework.remote.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.vsums")
public record VsumProperties(
    String vsumProvidersDir,
    String vsumProviderJar,
    String vsumProviderLibDirName,
    String vsumStorageDir
) {
}
