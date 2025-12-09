package tools.vitruv.framework.remote.server.http.java;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import tools.vitruv.framework.remote.server.rest.PathEndpointCollector;

/**
 * A wrapper around a {@link HttpServer} to process requests coming for a set of endpoints.
 */
public class VitruvJavaHttpServer {
  private final HttpServer server;

  /**
   * Creates a new server reachable from the address <code>http://host:port</code>,
   * which handles all requests under <code>endpoints</code>.
   *
   * @param host - String
   * @param port - int
   * @param endpoints - {@link List}
   * @throws IOException if server creation fails.
   */
  public VitruvJavaHttpServer(String host, int port, List<PathEndpointCollector> endpoints)
      throws IOException {
    this.server = HttpServer.create(new InetSocketAddress(host, port), 0);
    endpoints.forEach(endp -> server.createContext(endp.path(), new RequestHandler(endp)));
  }

  /**
   * Starts the server.
   */
  public void start() {
    server.start();
  }

  /**
   * Immediately stops the server, cancelling all running requests.
   */
  public void stop() {
    server.stop(0);
  }
}
