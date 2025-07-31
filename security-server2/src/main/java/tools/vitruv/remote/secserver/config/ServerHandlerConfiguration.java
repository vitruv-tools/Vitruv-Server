package tools.vitruv.remote.secserver.config;

import java.util.List;
import java.util.Set;

public record ServerHandlerConfiguration(Set<String> allowedOriginPatterns, ServerModes mode, List<AvailableHttpVersions> httpVersions) {
}
