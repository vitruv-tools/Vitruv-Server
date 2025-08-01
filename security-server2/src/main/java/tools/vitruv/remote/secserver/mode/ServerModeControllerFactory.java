package tools.vitruv.remote.secserver.mode;

import tools.vitruv.remote.secserver.config.ServerHandlerConfiguration;

public class ServerModeControllerFactory {
    private ServerModeControllerFactory() {}

    public static ServerModeController createModeController(ServerHandlerConfiguration config) {
        switch (config.mode()) {
            case PROXY:
                return new ProxyModeController(config);
            case REVERSE_PROXY:
                return new ReverseProxyModeController(config);
            default:
                throw new UnsupportedOperationException();
        }
    }
}
