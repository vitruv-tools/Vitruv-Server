package tools.vitruv.framework.remote.client.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.vitruv.change.interaction.InteractionFactory;
import tools.vitruv.change.interaction.InternalUserInteractor;
import tools.vitruv.framework.remote.client.exception.BadServerResponseException;
import tools.vitruv.framework.remote.common.rest.TaskState;
import tools.vitruv.framework.remote.common.rest.TaskStatus;
import tools.vitruv.framework.remote.server.VitruvServer;
import tools.vitruv.framework.views.CommittableView;
import tools.vitruv.framework.views.View;
import tools.vitruv.framework.views.ViewSelector;
import tools.vitruv.framework.views.ViewTypeFactory;
import tools.vitruv.framework.vsum.VirtualModel;
import tools.vitruv.framework.vsum.VirtualModelBuilder;

/**
 * Round trip through a real {@link VitruvServer}: changes committed asynchronously are propagated
 * in the background, and the client can follow them by their task id.
 */
class AsyncChangePropagationTest {
  private static final String VIEW_TYPE_NAME = "everything";
  private static final String EXTENSION = "ecore";
  private static final Duration TIMEOUT = Duration.ofSeconds(30);

  private static boolean registeredFactory;

  @TempDir Path serverDir;
  @TempDir Path clientDir;

  private VitruvServer server;
  private VitruvRemoteConnection client;

  /** See RemoteViewTypeTest: a real VSUM registers its file extensions, here it is done by hand. */
  @BeforeAll
  static void registerResourceFactory() {
    registeredFactory =
        Resource.Factory.Registry.INSTANCE
                .getExtensionToFactoryMap()
                .putIfAbsent(EXTENSION, new XMIResourceFactoryImpl())
            == null;
  }

  @AfterAll
  static void unregisterResourceFactory() {
    if (registeredFactory) {
      Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().remove(EXTENSION);
    }
  }

  @BeforeEach
  void startServer() throws IOException {
    int port = freePort();
    server =
        new VitruvServer(
            () -> {
              try {
                VirtualModel model =
                    new VirtualModelBuilder()
                        .withStorageFolder(serverDir)
                        .withUserInteractor(mock(InternalUserInteractor.class))
                        .withViewType(ViewTypeFactory.createIdentityMappingViewType(VIEW_TYPE_NAME))
                        .buildAndInitialize();
                seedOneRoot(model);
                return model;
              } catch (IOException e) {
                throw new IllegalStateException(e);
              }
            },
            port,
            "localhost");
    server.start();
    client = new VitruvRemoteConnection("http", "localhost", port, clientDir);
  }

  @AfterEach
  void stopServer() {
    server.stop();
  }

  @Test
  void commitChangesAsync_propagatesTheChangeInTheBackground() throws Exception {
    int taskId;
    try (var view = (ChangeRecordingRemoteView) openFullView().withChangeRecordingTrait()) {
      ((EPackage) view.getRootObjects().iterator().next()).setName("renamed");
      taskId = view.commitChangesAsync();
    }

    assertEquals(new TaskStatus(taskId, TaskState.DONE, null, null), awaitDone(taskId));
    EPackage root = (EPackage) openFullView().getRootObjects().iterator().next();
    assertEquals("renamed", root.getName());
  }

  @Test
  void answersForAFinishedChange_areRejected() throws Exception {
    int taskId;
    try (var view = (ChangeRecordingRemoteView) openFullView().withChangeRecordingTrait()) {
      ((EPackage) view.getRootObjects().iterator().next()).setName("renamed");
      taskId = view.commitChangesAsync();
    }
    awaitDone(taskId);
    var answer = InteractionFactory.eINSTANCE.createConfirmationUserInteraction();
    answer.setConfirmed(true);

    var exception =
        assertThrows(
            BadServerResponseException.class,
            () -> client.registerUserInteractions(taskId, List.of(answer)));

    assertEquals(HttpURLConnection.HTTP_CONFLICT, exception.getStatusCode());
  }

  @Test
  void statusOfAnUnknownTask_isNotFound() {
    var exception =
        assertThrows(BadServerResponseException.class, () -> client.getTaskStatus(4711));

    assertEquals(HttpURLConnection.HTTP_NOT_FOUND, exception.getStatusCode());
  }

  private TaskStatus awaitDone(int taskId) {
    long deadline = System.nanoTime() + TIMEOUT.toNanos();
    TaskStatus status = client.getTaskStatus(taskId);
    while (status.state() != TaskState.DONE) {
      if (status.state() == TaskState.FAILED || System.nanoTime() > deadline) {
        fail("Change was not propagated: " + status);
      }
      status = client.getTaskStatus(taskId);
    }
    return status;
  }

  private View openFullView() {
    RemoteViewType viewType =
        client.getViewTypes().stream()
            .filter(type -> VIEW_TYPE_NAME.equals(type.getName()))
            .map(RemoteViewType.class::cast)
            .findFirst()
            .orElseThrow();
    ViewSelector selector = client.createSelector(viewType);
    selector.getSelectableElements().forEach(element -> selector.setSelected(element, true));
    View view = selector.createView();
    assertTrue(view.getRootObjects().size() > 0, "the seeded root should be in the view");
    return view;
  }

  /** Puts a single EPackage into the VSUM so that views have something to select. */
  private void seedOneRoot(VirtualModel model) {
    var selector = model.createSelector(ViewTypeFactory.createIdentityMappingViewType("seeding"));
    try (CommittableView view = selector.createView().withChangeRecordingTrait()) {
      EPackage root = EcoreFactory.eINSTANCE.createEPackage();
      root.setName("seeded");
      view.registerRoot(
          root, URI.createFileURI(serverDir.resolve("seeded." + EXTENSION).toString()));
      view.commitChanges();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private static int freePort() throws IOException {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    }
  }
}
