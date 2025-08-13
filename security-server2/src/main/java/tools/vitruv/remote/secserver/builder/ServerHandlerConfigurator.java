package tools.vitruv.remote.secserver.builder;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jetty.security.openid.OpenIdConfiguration;

import tools.vitruv.framework.remote.server.VirtualModelInitializer;
import tools.vitruv.remote.secserver.VitruvSecurityServer2;
import tools.vitruv.remote.secserver.config.AuthenticationMode;
import tools.vitruv.remote.secserver.config.ServerConfiguration;
import tools.vitruv.remote.secserver.config.ServerConnectionConfiguration;
import tools.vitruv.remote.secserver.config.ServerHandlerConfiguration;

public abstract class ServerHandlerConfigurator {
    private ServerConnectionConfiguration connectionConfig;
    private Set<String> allowedOriginPatterns = new HashSet<>();
    private AuthenticationMode authMethod;
    private OpenIdConfiguration openIdConfig;

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

    public ServerHandlerConfigurator authenticateWith(AuthenticationMode authMethod) {
        this.authMethod = authMethod;
        return this;
    }

    AuthenticationMode getAuthenticationMode() {
        return this.authMethod;
    }

    public ServerHandlerConfigurator withOpenIdConfiguration(String discoveryUri, String clientId, String clientSecret) {
        this.openIdConfig = new OpenIdConfiguration(discoveryUri, clientId, clientSecret);
        return this;
    }

    public ServerHandlerConfigurator withOpenIdConfiguration(OpenIdConfiguration openIdConfig) {
        this.openIdConfig = openIdConfig;
        return this;
    }

    OpenIdConfiguration getOpenIdConfig() {
        return this.openIdConfig;
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
