package tools.vitruv.framework.remote.server.jetty;

import java.util.List;

import tools.vitruv.framework.remote.server.VitruvServer;
import tools.vitruv.framework.remote.server.VitruvServerConfiguration;
import tools.vitruv.framework.remote.server.http.InternalHttpServerManager;
import tools.vitruv.framework.remote.server.http.jetty.VitruvJettyServerManager;
import tools.vitruv.framework.remote.server.rest.PathEndointCollector;

public class JettyVitruvServer extends VitruvServer {
    public JettyVitruvServer(VitruvServerConfiguration config) {
        super(config);
    }
    
    @Override
    protected InternalHttpServerManager createInternalServerManager(VitruvServerConfiguration config,
            List<PathEndointCollector> endpoints) throws Exception {
        return new VitruvJettyServerManager(config.hostOrIp(), config.port(), endpoints);
    }
}
