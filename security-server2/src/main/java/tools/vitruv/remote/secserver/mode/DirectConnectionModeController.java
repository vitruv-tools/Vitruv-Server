package tools.vitruv.remote.secserver.mode;

import org.eclipse.jetty.server.Handler;

import tools.vitruv.framework.remote.common.json.JsonMapper;
import tools.vitruv.framework.remote.server.VirtualModelInitializer;
import tools.vitruv.framework.remote.server.http.jetty.JettyHandlerFactory;
import tools.vitruv.framework.remote.server.rest.endpoints.EndpointsProvider;
import tools.vitruv.remote.secserver.config.ServerHandlerConfiguration;

public class DirectConnectionModeController extends AbstractProxyModeController {
    private Handler directHandler;

    DirectConnectionModeController(ServerHandlerConfiguration config) {
        super(config);
    }

    @Override
    public void initialize(VirtualModelInitializer modelInitializer) throws Exception {
        var model = modelInitializer.init();
        var mapper = new JsonMapper(model.getFolder());
        var endpoints = EndpointsProvider.getAllEndpoints(model, mapper);
        this.directHandler = JettyHandlerFactory.createHandler(endpoints);
    }

    @Override
    public Handler getHandler() {
        return this.directHandler;
    }
}
