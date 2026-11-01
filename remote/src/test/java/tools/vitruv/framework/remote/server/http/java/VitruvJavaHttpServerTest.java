package tools.vitruv.framework.remote.server.http.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.vitruv.change.utils.ProjectMarker;
import tools.vitruv.framework.remote.common.json.JsonMapper;
import tools.vitruv.framework.remote.common.rest.TaskState;
import tools.vitruv.framework.remote.common.rest.TaskStatus;
import tools.vitruv.framework.remote.common.rest.constants.EndpointPath;
import tools.vitruv.framework.remote.common.rest.constants.Header;
import tools.vitruv.framework.remote.server.propagation.PropagationWorker;
import tools.vitruv.framework.remote.server.rest.GetEndpoint;
import tools.vitruv.framework.remote.server.rest.PathEndointCollector;
import tools.vitruv.framework.remote.server.rest.endpoints.EndpointsProvider;
import tools.vitruv.framework.vsum.VirtualModel;

/** Tests the {@link VitruvJavaHttpServer} with the endpoints of the Vitruvius server. */
class VitruvJavaHttpServerTest {
  private static final Duration TIMEOUT = Duration.ofSeconds(10);
  private static final int TASK_ID = 7;

  @TempDir Path vsumFolder;

  private final Object vsumLock = new Object();
  private final PropagationWorker worker = mock(PropagationWorker.class);
  private final HttpClient client = HttpClient.newHttpClient();
  private final CountDownLatch waitingForVsum = new CountDownLatch(1);
  private VitruvJavaHttpServer server;
  private String baseUri;

  @BeforeEach
  void startServer() throws IOException {
    ProjectMarker.markAsProjectRootFolder(vsumFolder);
    List<PathEndointCollector> endpoints =
        EndpointsProvider.getAllEndpoints(
                mock(VirtualModel.class), new JsonMapper(vsumFolder), vsumLock, worker)
            .stream()
            .map(it -> it.path().equals(EndpointPath.VIEW_TYPES) ? signalWaitingForVsum(it) : it)
            .toList();
    int port;
    try (ServerSocket socket = new ServerSocket(0)) {
      port = socket.getLocalPort();
    }
    server = new VitruvJavaHttpServer("localhost", port, endpoints);
    server.start();
    baseUri = "http://localhost:" + port;
  }

  @AfterEach
  void stopServer() {
    server.stop();
  }

  @Test
  void requestsWithoutTheVsum_areAnsweredWhileAnotherRequestWaitsForIt() throws Exception {
    var status = new TaskStatus(TASK_ID, TaskState.QUEUED, null, null);
    when(worker.getStatus(TASK_ID)).thenReturn(Optional.of(status));
    CompletableFuture<HttpResponse<String>> viewTypes;
    // Stands in for the propagation worker running a long step.
    synchronized (vsumLock) {
      viewTypes =
          client.sendAsync(request(EndpointPath.VIEW_TYPES).GET().build(), BodyHandlers.ofString());
      assertTrue(waitingForVsum.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));

      var taskResponse =
          client.send(
              request(EndpointPath.TASK).header(Header.TASK_ID, "7").GET().build(),
              BodyHandlers.ofString());
      var healthResponse =
          client.send(request(EndpointPath.HEALTH).GET().build(), BodyHandlers.ofString());

      assertEquals(HttpURLConnection.HTTP_OK, taskResponse.statusCode());
      assertEquals(
          status, new JsonMapper(vsumFolder).deserialize(taskResponse.body(), TaskStatus.class));
      assertEquals(HttpURLConnection.HTTP_OK, healthResponse.statusCode());
      assertFalse(viewTypes.isDone(), "the request that needs the VSUM still waits for it");
    }

    assertEquals(
        HttpURLConnection.HTTP_OK,
        viewTypes.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS).statusCode());
  }

  private HttpRequest.Builder request(String path) {
    return HttpRequest.newBuilder(URI.create(baseUri + path)).timeout(TIMEOUT);
  }

  /** Signals when a request has arrived at the endpoint, right before it waits for the VSUM. */
  private PathEndointCollector signalWaitingForVsum(PathEndointCollector endpoints) {
    GetEndpoint signalling =
        wrapper -> {
          waitingForVsum.countDown();
          return endpoints.getEndpoint().process(wrapper);
        };
    return new PathEndointCollector(
        endpoints.path(),
        signalling,
        endpoints.postEndpoint(),
        endpoints.putEndpoint(),
        endpoints.patchEndpoint(),
        endpoints.deleteEndpoint());
  }
}
