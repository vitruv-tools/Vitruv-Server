package tools.vitruv.remote.secserver.builder;

import java.util.HashMap;
import java.util.Map;

import tools.vitruv.remote.secserver.config.ServerConnectionConfiguration;
import tools.vitruv.remote.secserver.config.ServerHandlerConfiguration;
import tools.vitruv.remote.secserver.config.ServerModes;

public class ReverseProxyModeConfigurator extends ServerHandlerConfigurator {
    private ServerConnectionConfiguration connectionConfig;
    private Map<String, String> pathToUri = new HashMap<>();

    ReverseProxyModeConfigurator(ServerConnectionConfiguration connectionConfig) {
        super(connectionConfig);
        this.connectionConfig = connectionConfig;
    }

    public ReverseProxyModeConfigurator addPathForRedirection(String path, String redirectUri) {
        this.pathToUri.put(path, redirectUri);
        return this;
    }

    @Override
    ServerHandlerConfiguration getServerHandlerConfiguration() {
        return new ServerHandlerConfiguration(
            getAllowedOriginPatterns(),
            ServerModes.REVERSE_PROXY,
            this.connectionConfig.httpVersions(),
            null,
            this.pathToUri,
            this.getAuthenticationMode(),
            this.getOpenIdConfig()
        );
    }
}
