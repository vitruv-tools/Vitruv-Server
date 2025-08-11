package tools.vitruv.remote.secserver.builder;

import tools.vitruv.remote.secserver.config.ServerConnectionConfiguration;
import tools.vitruv.remote.secserver.config.ServerHandlerConfiguration;
import tools.vitruv.remote.secserver.config.ServerModes;

public class DirectConnectionModeConfigurator extends ServerHandlerConfigurator {
    private ServerConnectionConfiguration connectionConfig;

    DirectConnectionModeConfigurator(ServerConnectionConfiguration connectionConfig) {
        super(connectionConfig);
        this.connectionConfig = connectionConfig;
    }

    @Override
    ServerHandlerConfiguration getServerHandlerConfiguration() {
        return new ServerHandlerConfiguration(
            getAllowedOriginPatterns(),
            ServerModes.DIRECT_CONNECTION,
            this.connectionConfig.httpVersions(),
            null,
            null,
            this.getAuthenticationMode(),
            this.getOpenIdConfig()
        );
    }
}
