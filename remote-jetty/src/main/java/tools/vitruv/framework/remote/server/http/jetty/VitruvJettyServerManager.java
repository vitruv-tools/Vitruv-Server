package tools.vitruv.framework.remote.server.http.jetty;

import java.util.List;

import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

import tools.vitruv.framework.remote.server.http.InternalHttpServerManager;
import tools.vitruv.framework.remote.server.rest.PathEndointCollector;

public class VitruvJettyServerManager implements InternalHttpServerManager {
    private Server server = new Server();
    private int boundPort;

    public VitruvJettyServerManager(String hostOrIp, int port, List<PathEndointCollector> endpoints) {
        this.boundPort = port;

        HttpConfiguration httpConfig = new HttpConfiguration();

        HttpConnectionFactory http1 = new HttpConnectionFactory(httpConfig);
        HTTP2CServerConnectionFactory h2c = new HTTP2CServerConnectionFactory(httpConfig);
        
        ServerConnector connector = new ServerConnector(this.server, http1, h2c);
        connector.setHost(hostOrIp);
        connector.setPort(port);
        connector.addEventListener(new NetworkConnector.Listener() {
            @Override
            public void onOpen(NetworkConnector connector) {
                boundPort = connector.getLocalPort();
            }
        });
        this.server.addConnector(connector);
        
        this.server.setHandler(JettyHandlerFactory.createHandler(endpoints));
    }

    @Override
    public int getBoundPort() {
        return this.boundPort;
    }

    @Override
    public void start() throws Exception {
        this.server.start();
    }

    @Override
    public void stop() throws Exception {
        this.server.stop();
    }
}
