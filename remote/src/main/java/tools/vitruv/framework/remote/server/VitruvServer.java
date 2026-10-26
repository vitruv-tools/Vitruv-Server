package tools.vitruv.framework.remote.server;

import java.io.IOException;
import java.util.List;
import tools.vitruv.framework.remote.common.DefaultConnectionSettings;
import tools.vitruv.framework.remote.common.json.JsonMapper;
import tools.vitruv.framework.remote.server.http.java.VitruvJavaHttpServer;
import tools.vitruv.framework.remote.server.propagation.PropagationWorker;
import tools.vitruv.framework.remote.server.rest.PathEndointCollector;
import tools.vitruv.framework.remote.server.rest.endpoints.EndpointsProvider;
import tools.vitruv.framework.vsum.VirtualModel;

/**
 * A Vitruvius server wraps a REST-based API around a {@link VirtualModel VSUM}. Therefore, it takes
 * a {@link VirtualModelInitializer} which is responsible to create an instance of a {@link
 * VirtualModel virtual model}. Once the server is started, the API can be used by the Vitruvius
 * client to perform remote actions on the VSUM.
 *
 * <p>Requests that work on the VSUM or its views are handled one at a time. Changes that a client
 * sends for asynchronous propagation are propagated by a {@link PropagationWorker} on a background
 * thread instead, so the server keeps answering requests while they are propagated. The health
 * check and the requests about such changes are answered right away, also while another request
 * waits for the VSUM.
 */
public class VitruvServer {
  private final VitruvJavaHttpServer server;
  private final PropagationWorker propagationWorker;

  /**
   * Creates a new {@link VitruvServer} using the given {@link VirtualModelInitializer}. Sets host
   * name or IP address and port which are used to open the server.
   *
   * @param modelInitializer The initializer which creates an {@link VirtualModel}.
   * @param port The port to open to server on.
   * @param hostOrIp The host name or IP address to which the server is bound.
   */
  public VitruvServer(VirtualModelInitializer modelInitializer, int port, String hostOrIp)
      throws IOException {
    VirtualModel model = modelInitializer.init();
    JsonMapper mapper = new JsonMapper(model.getFolder());
    // The request handler and the propagation worker run on different threads. Whoever works on
    // the VSUM or its views holds this lock, so they never get in each other's way.
    Object vsumLock = new Object();
    this.propagationWorker = new PropagationWorker(model, vsumLock);
    List<PathEndointCollector> endpoints =
        EndpointsProvider.getAllEndpoints(model, mapper, vsumLock, propagationWorker);

    this.server = new VitruvJavaHttpServer(hostOrIp, port, endpoints);
  }

  /**
   * Creates a new {@link VitruvServer} using the given {@link VirtualModelInitializer}. Sets the
   * port which is used to open the server on to the given one.
   *
   * @param modelInitializer The initializer which creates an {@link VirtualModel}.
   * @param port The port to open to server on.
   */
  public VitruvServer(VirtualModelInitializer modelInitializer, int port) throws IOException {
    this(modelInitializer, port, DefaultConnectionSettings.STD_HOST);
  }

  /**
   * Creates a new {@link VitruvServer} using the given {@link VirtualModelInitializer}. Sets the
   * port which is used to open the server on to 8080.
   *
   * @param modelInitializer The initializer which creates an {@link
   *     tools.vitruv.framework.vsum.internal.InternalVirtualModel}.
   */
  public VitruvServer(VirtualModelInitializer modelInitializer) throws IOException {
    this(modelInitializer, DefaultConnectionSettings.STD_PORT);
  }

  /** Starts the Vitruvius server and the worker that propagates changes asynchronously. */
  public void start() {
    propagationWorker.start();
    server.start();
  }

  /**
   * Stops the Vitruvius server. It stops accepting requests and waits for the requests that are
   * being handled first, then stops the propagation worker once the step it is running, if any, has
   * finished. When this returns, neither touches the VSUM anymore.
   */
  public void stop() {
    server.stop();
    propagationWorker.stop();
  }
}
