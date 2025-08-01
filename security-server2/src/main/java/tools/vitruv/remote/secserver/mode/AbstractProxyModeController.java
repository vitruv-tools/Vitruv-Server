package tools.vitruv.remote.secserver.mode;

import org.eclipse.jetty.server.Handler;

import tools.vitruv.remote.secserver.config.ServerHandlerConfiguration;
import tools.vitruv.remote.secserver.handler.VitruvReverseProxyHandler;

abstract class AbstractProxyModeController implements ServerModeController {
    private ServerHandlerConfiguration config;

    AbstractProxyModeController(ServerHandlerConfiguration config) {
        this.config = config;
    }

    @Override
    public Handler getHandler() {
        return new VitruvReverseProxyHandler(this.config.httpVersions());
    }

    @Override
    public String getBaseUrl() {
        return "";
    }

    @Override
    public void start() {}

    @Override
    public void stop() {}
}
