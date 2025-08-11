package tools.vitruv.remote.secserver.config;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jetty.security.openid.OpenIdConfiguration;

import tools.vitruv.framework.remote.server.VitruvServerConfiguration;

public record ServerHandlerConfiguration(
    Set<String> allowedOriginPatterns,
    ServerModes mode,
    List<AvailableHttpVersions> httpVersions,
    VitruvServerConfiguration proxiedServerConfig,
    Map<String, String> initialPathUriRedirects,
    AuthenticationMode authMethod,
    OpenIdConfiguration openIdConfig) {
}
