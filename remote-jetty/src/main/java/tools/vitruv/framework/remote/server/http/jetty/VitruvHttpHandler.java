package tools.vitruv.framework.remote.server.http.jetty;

import tools.vitruv.framework.remote.server.http.AbstractHttpHandler;
import tools.vitruv.framework.remote.server.http.HttpWrapper;
import tools.vitruv.framework.remote.server.rest.PathEndointCollector;

class VitruvHttpHandler extends AbstractHttpHandler {
    VitruvHttpHandler(PathEndointCollector endpoints) {
        super(endpoints);
    }

    @Override
    public void process(String method, HttpWrapper wrapper) {
        super.process(method, wrapper);
    }
}
