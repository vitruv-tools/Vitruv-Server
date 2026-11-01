package tools.vitruv.framework.remote.server.http.java;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import tools.vitruv.framework.remote.server.rest.PathEndointCollector;

/**
 * An HTTP server for the Vitruvius endpoints, based on the HTTP server built into the JDK.
 *
 * <p>Every request is handled on a thread of its own. Endpoints that work on the VSUM serialize
 * themselves on the VSUM lock, so a request that waits for that lock does not hold up requests to
 * endpoints that never take it, such as the health check.
 */
public class VitruvJavaHttpServer {
  private final HttpServer server;
  private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

  /**
   * Creates a Vitruvius HTTP server on the given host and port, registering the given endpoints.
   */
  public VitruvJavaHttpServer(String host, int port, List<PathEndointCollector> endpoints)
      throws IOException {
    this.server = HttpServer.create(new InetSocketAddress(host, port), 0);
    endpoints.forEach(endp -> server.createContext(endp.path(), new RequestHandler(endp)));
    server.setExecutor(executor);
  }

  /** Starts the Vitruvius server. */
  public void start() {
    server.start();
  }

  /** Stops the Vitruvius server and waits for the requests that are being handled to finish. */
  public void stop() {
    server.stop(0);
    executor.close();
  }
}
