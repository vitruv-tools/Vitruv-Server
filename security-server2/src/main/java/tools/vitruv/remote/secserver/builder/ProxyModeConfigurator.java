package tools.vitruv.remote.secserver.builder;

import tools.vitruv.framework.remote.common.DefaultConnectionSettings;
import tools.vitruv.framework.remote.server.VitruvServerConfiguration;
import tools.vitruv.remote.secserver.config.ServerConnectionConfiguration;
import tools.vitruv.remote.secserver.config.ServerHandlerConfiguration;
import tools.vitruv.remote.secserver.config.ServerModes;

public class ProxyModeConfigurator extends ServerHandlerConfigurator {
    private ServerConnectionConfiguration connectionConfig;
    private int vitruvServerPort;

    ProxyModeConfigurator(ServerConnectionConfiguration connectionConfig) {
        super(connectionConfig);
        this.connectionConfig = connectionConfig;
    }

    public ProxyModeConfigurator runVitruvServerOnPort(int port) {
        this.vitruvServerPort = port;
        return this;
    }

    public ProxyModeConfigurator runVitruvServerOnRandomPort() {
        this.vitruvServerPort = 0;
        return this;
    }

    @Override
    ServerHandlerConfiguration getServerHandlerConfiguration() {
        return new ServerHandlerConfiguration(
            this.getAllowedOriginPatterns(),
            ServerModes.PROXY,
            this.connectionConfig.httpVersions(),
            new VitruvServerConfiguration(DefaultConnectionSettings.STD_HOST, this.vitruvServerPort),
            null
        );
    }
}
