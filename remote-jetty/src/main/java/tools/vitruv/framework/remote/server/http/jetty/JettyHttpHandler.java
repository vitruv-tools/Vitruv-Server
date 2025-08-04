package tools.vitruv.framework.remote.server.http.jetty;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import tools.vitruv.framework.remote.server.http.VitruvHttpHandler;
import tools.vitruv.framework.remote.server.rest.PathEndointCollector;

public class JettyHttpHandler extends Handler.Abstract {
    private VitruvHttpHandler actualHandler;

    JettyHttpHandler(PathEndointCollector endpoints) {
        this.actualHandler = new VitruvHttpHandler(endpoints);
    }

    @Override
    public boolean handle(Request request, Response response, Callback callback) throws Exception {
        this.actualHandler.process(request.getMethod(), new JettyHttpWrapper(request, response, callback));
        return true;
    }
}
