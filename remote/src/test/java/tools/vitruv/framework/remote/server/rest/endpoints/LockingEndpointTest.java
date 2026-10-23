package tools.vitruv.framework.remote.server.rest.endpoints;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.vitruv.change.utils.ProjectMarker;
import tools.vitruv.framework.remote.common.json.JsonMapper;
import tools.vitruv.framework.remote.common.rest.constants.EndpointPath;
import tools.vitruv.framework.remote.server.exception.ServerHaltingException;
import tools.vitruv.framework.remote.server.propagation.PropagationWorker;
import tools.vitruv.framework.remote.server.rest.PathEndointCollector;
import tools.vitruv.framework.vsum.VirtualModel;

class LockingEndpointTest {
  private final Object vsumLock = new Object();

  @Test
  void process_runsTheEndpointWhileHoldingTheLock() {
    var endpoint =
        new LockingEndpoint(
            vsumLock, wrapper -> Thread.holdsLock(vsumLock) ? "lock held" : "lock not held");

    assertEquals("lock held", endpoint.process(new FakeHttpWrapper(null)));
    assertFalse(Thread.holdsLock(vsumLock));
  }

  @Test
  void process_passesErrorsOnAndReleasesTheLock() {
    var endpoint =
        new LockingEndpoint(
            vsumLock,
            wrapper -> {
              throw new ServerHaltingException(418, "nope");
            });

    var exception =
        assertThrows(
            ServerHaltingException.class, () -> endpoint.process(new FakeHttpWrapper(null)));

    assertEquals(418, exception.getStatusCode());
    assertFalse(Thread.holdsLock(vsumLock));
  }

  @Test
  void endpointsProvider_locksTheVsumEndpointsButNotHealthAndTasks(@TempDir Path vsumFolder)
      throws IOException {
    ProjectMarker.markAsProjectRootFolder(vsumFolder);
    PropagationWorker worker = mock(PropagationWorker.class);
    Map<String, PathEndointCollector> endpoints =
        EndpointsProvider.getAllEndpoints(
                mock(VirtualModel.class), new JsonMapper(vsumFolder), vsumLock, worker)
            .stream()
            .collect(Collectors.toMap(PathEndointCollector::path, Function.identity()));

    assertInstanceOf(HealthEndpoint.class, endpoints.get(EndpointPath.HEALTH).getEndpoint());
    assertInstanceOf(TaskStatusEndpoint.class, endpoints.get(EndpointPath.TASK).getEndpoint());
    assertInstanceOf(
        UserInteractionEndpoint.class, endpoints.get(EndpointPath.TASK).postEndpoint());
    // Propagation takes the lock itself, and only when it propagates before answering.
    assertInstanceOf(
        ChangePropagationEndpoint.class, endpoints.get(EndpointPath.VIEW).patchEndpoint());

    var view = endpoints.get(EndpointPath.VIEW);
    assertInstanceOf(LockingEndpoint.class, view.getEndpoint());
    assertInstanceOf(LockingEndpoint.class, view.postEndpoint());
    assertInstanceOf(LockingEndpoint.class, view.deleteEndpoint());
    assertInstanceOf(
        LockingEndpoint.class, endpoints.get(EndpointPath.CHANGE_DERIVING).patchEndpoint());
    for (String path :
        new String[] {
          EndpointPath.VIEW_TYPES,
          EndpointPath.VIEW_SELECTOR,
          EndpointPath.IS_VIEW_CLOSED,
          EndpointPath.IS_VIEW_OUTDATED
        }) {
      assertInstanceOf(LockingEndpoint.class, endpoints.get(path).getEndpoint(), path);
    }
    assertEquals(8, endpoints.size());
  }
}
