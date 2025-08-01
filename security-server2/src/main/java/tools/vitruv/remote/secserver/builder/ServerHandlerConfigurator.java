package tools.vitruv.remote.secserver.builder;

import java.util.HashSet;
import java.util.Set;

import tools.vitruv.framework.remote.server.VirtualModelInitializer;
import tools.vitruv.remote.secserver.VitruvSecurityServer2;
import tools.vitruv.remote.secserver.config.ServerConfiguration;
import tools.vitruv.remote.secserver.config.ServerConnectionConfiguration;
import tools.vitruv.remote.secserver.config.ServerHandlerConfiguration;

public abstract class ServerHandlerConfigurator {
    private ServerConnectionConfiguration connectionConfig;
    private Set<String> allowedOriginPatterns = new HashSet<>();

    ServerHandlerConfigurator(ServerConnectionConfiguration connectionConfig) {
        this.connectionConfig = connectionConfig;
    }

    public ServerHandlerConfigurator addAllowedOriginPattern(String pattern) {
        this.allowedOriginPatterns.add(pattern);
        return this;
    }

    Set<String> getAllowedOriginPatterns() {
        return this.allowedOriginPatterns;
    }

    abstract ServerHandlerConfiguration getServerHandlerConfiguration();

    public VitruvSecurityServer2 buildFor(VirtualModelInitializer vsumInitializer) throws Exception {
        var secServer = new VitruvSecurityServer2(
            new ServerConfiguration(
                this.connectionConfig,
                getServerHandlerConfiguration()
            )
        );
        secServer.initialize(vsumInitializer);
        return secServer;
    }
}
