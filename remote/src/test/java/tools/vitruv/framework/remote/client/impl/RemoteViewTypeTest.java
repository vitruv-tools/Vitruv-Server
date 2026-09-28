package tools.vitruv.framework.remote.client.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Path;
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
import tools.vitruv.change.interaction.InternalUserInteractor;
import tools.vitruv.framework.remote.server.VitruvServer;
import tools.vitruv.framework.views.CommittableView;
import tools.vitruv.framework.views.View;
import tools.vitruv.framework.views.ViewSelector;
import tools.vitruv.framework.views.ViewType;
import tools.vitruv.framework.views.ViewTypeFactory;
import tools.vitruv.framework.vsum.VirtualModel;
import tools.vitruv.framework.vsum.VirtualModelBuilder;

/**
 * Round trip through a real {@link VitruvServer}: a view obtained remotely reports the {@link
 * RemoteViewType} it was created from.
 */
class RemoteViewTypeTest {

  private static final String VIEW_TYPE_NAME = "everything";
  private static final String EXTENSION = "ecore";

  private static boolean registeredFactory;

  @TempDir Path serverDir;
  @TempDir Path clientDir;

  private VitruvServer server;
  private VitruvRemoteConnection client;

  /**
   * The server copies view resources into a plain resource set, which relies on EMF's global
   * factory registry; a real VSUM registers its file extensions, here it is done by hand.
   */
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
  void remoteView_reportsTheViewTypeItWasCreatedFrom() {
    RemoteViewType viewType = remoteViewType();

    View view = openFullView(viewType);

    assertSame(viewType, view.getViewType());
    assertEquals(VIEW_TYPE_NAME, view.getViewType().getName());
  }

  @Test
  void changeRecordingAndDerivingViews_reportTheSameViewType() throws Exception {
    RemoteViewType viewType = remoteViewType();
    View view = openFullView(viewType);

    try (CommittableView recording = view.withChangeRecordingTrait()) {
      assertSame(viewType, recording.getViewType());
    }
    View other = openFullView(viewType);
    try (CommittableView deriving = other.withChangeDerivingTrait()) {
      assertSame(viewType, deriving.getViewType());
    }
  }

  @Test
  void reportedViewType_createsSelectorsThatWorkAgainstTheServer() {
    View view = openFullView(remoteViewType());

    ViewType<? extends ViewSelector> reported = view.getViewType();
    ViewSelector again = client.createSelector(reported);

    assertEquals(1, again.getSelectableElements().size());
    again.getSelectableElements().forEach(element -> again.setSelected(element, true));
    View reopened = again.createView();
    assertEquals(1, reopened.getRootObjects().size());
    assertSame(reported, reopened.getViewType());
  }

  private RemoteViewType remoteViewType() {
    return client.getViewTypes().stream()
        .filter(type -> VIEW_TYPE_NAME.equals(type.getName()))
        .map(RemoteViewType.class::cast)
        .findFirst()
        .orElseThrow();
  }

  private View openFullView(RemoteViewType viewType) {
    ViewSelector selector = client.createSelector(viewType);
    selector.getSelectableElements().forEach(element -> selector.setSelected(element, true));
    View view = selector.createView();
    assertTrue(view.getRootObjects().size() > 0, "the seeded root should be in the view");
    return view;
  }

  /** Puts a single EPackage into the VSUM so that views have something to select. */
  private void seedOneRoot(VirtualModel model) {
    var selector =
        model.createSelector(ViewTypeFactory.createIdentityMappingViewType("seeding"));
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
