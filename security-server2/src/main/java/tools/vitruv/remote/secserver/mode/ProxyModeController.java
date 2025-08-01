package tools.vitruv.remote.secserver.mode;

import tools.vitruv.framework.remote.server.VirtualModelInitializer;
import tools.vitruv.framework.remote.server.VitruvServer;
import tools.vitruv.remote.secserver.config.ServerHandlerConfiguration;
import tools.vitruv.remote.secserver.proxy.ReverseProxyMappingService;

class ProxyModeController extends AbstractProxyModeController {
    private ServerHandlerConfiguration config;
    private VitruvServer proxiedServer;

    ProxyModeController(ServerHandlerConfiguration config) {
        super(config);
        this.config = config;
    }

    @Override
    public void initialize(VirtualModelInitializer modelInitializer) throws Exception {
        this.proxiedServer = new VitruvServer(this.config.proxiedServerConfig());
        this.proxiedServer.initialize(modelInitializer);
    }

    @Override
    public String getBaseUrl() {
        return this.proxiedServer.getBaseUrl();
    }

    @Override
    public void start() {
        this.proxiedServer.start();
        ReverseProxyMappingService.instance().setOneDestination(this.proxiedServer.getBaseUrl());
    }

    @Override
    public void stop() {
        this.proxiedServer.stop();
    }
}
