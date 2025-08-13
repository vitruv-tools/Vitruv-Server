package tools.vitruv.framework.remote.server.http.jetty;

import java.util.List;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;

import tools.vitruv.framework.remote.server.rest.PathEndointCollector;

public final class JettyHandlerFactory {
    private JettyHandlerFactory() {}

    public static Handler createHandler(List<PathEndointCollector> endpoints) {
        ContextHandlerCollection handler = new ContextHandlerCollection();
        endpoints.forEach((ep) -> {
            var contextHandler = new ContextHandler(new JettyHttpHandler(ep), ep.path());
            contextHandler.setAllowNullPathInContext(true);
            handler.addHandler(contextHandler);
        });
        return handler;
    }
}
