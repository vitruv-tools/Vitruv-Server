package tools.vitruv.remote.secserver.mode;

import tools.vitruv.framework.remote.server.VirtualModelInitializer;
import tools.vitruv.remote.secserver.config.ServerHandlerConfiguration;
import tools.vitruv.remote.secserver.proxy.ReverseProxyMappingService;

class ReverseProxyModeController extends AbstractProxyModeController {
    private ServerHandlerConfiguration config;

    ReverseProxyModeController(ServerHandlerConfiguration config) {
        super(config);
        this.config = config;
    }

    @Override
    public void initialize(VirtualModelInitializer modelInitializer) throws Exception {
        for (var entry : this.config.initialPathUriRedirects().entrySet()) {
            ReverseProxyMappingService.instance().addDestination(entry.getKey(), entry.getValue());
        }
    }
}
