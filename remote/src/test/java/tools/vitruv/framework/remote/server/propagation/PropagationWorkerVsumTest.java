package tools.vitruv.framework.remote.server.propagation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.vitruv.change.atomic.hid.HierarchicalId;
import tools.vitruv.change.composite.description.VitruviusChange;
import tools.vitruv.change.composite.description.VitruviusChangeResolverFactory;
import tools.vitruv.change.composite.recording.ChangeRecorder;
import tools.vitruv.change.interaction.InternalUserInteractor;
import tools.vitruv.framework.remote.common.rest.TaskState;
import tools.vitruv.framework.views.CommittableView;
import tools.vitruv.framework.views.View;
import tools.vitruv.framework.views.ViewSelector;
import tools.vitruv.framework.views.ViewType;
import tools.vitruv.framework.views.ViewTypeFactory;
import tools.vitruv.framework.views.impl.ModifiableView;
import tools.vitruv.framework.views.impl.ViewCreatingViewType;
import tools.vitruv.framework.vsum.VirtualModel;
import tools.vitruv.framework.vsum.VirtualModelBuilder;

/** Tests the {@link PropagationWorker} against a real VSUM that resolves and propagates changes. */
class PropagationWorkerVsumTest {
  private static final String EXTENSION = "ecore";
  private static final Duration TIMEOUT = Duration.ofSeconds(30);

  private static boolean registeredFactory;

  @TempDir Path vsumFolder;

  private final ViewType<?> viewType = ViewTypeFactory.createIdentityMappingViewType("everything");
  private VirtualModel vsum;
  private PropagationWorker worker;

  /** See AsyncChangePropagationTest: a real VSUM registers its file extensions. */
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
  void createVsumWithOneClass() throws IOException {
    vsum =
        new VirtualModelBuilder()
            .withStorageFolder(vsumFolder)
            .withUserInteractor(mock(InternalUserInteractor.class))
            .withViewType(viewType)
            .buildAndInitialize();
    var selector = vsum.createSelector(ViewTypeFactory.createIdentityMappingViewType("seeding"));
    try (CommittableView view = selector.createView().withChangeRecordingTrait()) {
      EPackage root = EcoreFactory.eINSTANCE.createEPackage();
      root.setName("seeded");
      EClass alpha = EcoreFactory.eINSTANCE.createEClass();
      alpha.setName("Alpha");
      root.getEClassifiers().add(alpha);
      view.registerRoot(
          root, URI.createFileURI(vsumFolder.resolve("seeded." + EXTENSION).toString()));
      view.commitChanges();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
    worker = new PropagationWorker(vsum, new Object());
  }

  @AfterEach
  void stopWorker() {
    worker.stop();
  }

  @SuppressWarnings("unchecked")
  @Test
  void consecutiveChangesOfOneView_areResolvedAgainstTheChangesBeforeThem() {
    ModifiableView view = (ModifiableView) openFullView();
    ResourceSet[] viewResourceSet = new ResourceSet[1];
    view.modifyContents(resourceSet -> viewResourceSet[0] = resourceSet);
    VitruviusChange<HierarchicalId> first;
    VitruviusChange<HierarchicalId> second;
    try (ChangeRecorder recorder = new ChangeRecorder(viewResourceSet[0])) {
      recorder.addToRecording(viewResourceSet[0]);
      EPackage root = (EPackage) view.getRootObjects().iterator().next();
      // The first change renames Alpha and creates a new class with its old name ...
      recorder.beginRecording();
      root.getEClassifier("Alpha").setName("Beta");
      EClass newAlpha = EcoreFactory.eINSTANCE.createEClass();
      newAlpha.setName("Alpha");
      root.getEClassifiers().add(newAlpha);
      first = assignIds(recorder.endRecording(), viewResourceSet[0]);
      // ... and the second one changes that new class, which the VSUM only has after the first.
      recorder.beginRecording();
      newAlpha.setAbstract(true);
      second = assignIds(recorder.endRecording(), viewResourceSet[0]);
    }
    var type = (ViewCreatingViewType<?, HierarchicalId>) view.getViewType();
    // Both are there when the worker starts, as when it is busy while the client commits twice.
    int firstTask = worker.submit(view, type, first);
    int secondTask = worker.submit(view, type, second);

    worker.start();

    awaitDone(firstTask);
    awaitDone(secondTask);
    EPackage root = (EPackage) openFullView().getRootObjects().iterator().next();
    assertTrue(((EClass) root.getEClassifier("Alpha")).isAbstract(), "the new class changed");
    assertFalse(((EClass) root.getEClassifier("Beta")).isAbstract(), "the renamed class did not");
  }

  private View openFullView() {
    ViewSelector selector = vsum.createSelector(viewType);
    selector.getSelectableElements().forEach(element -> selector.setSelected(element, true));
    return selector.createView();
  }

  private static VitruviusChange<HierarchicalId> assignIds(
      VitruviusChange<EObject> change, ResourceSet viewResourceSet) {
    return VitruviusChangeResolverFactory.forHierarchicalIds(viewResourceSet).assignIds(change);
  }

  private void awaitDone(int taskId) {
    long deadline = System.nanoTime() + TIMEOUT.toNanos();
    var status = worker.getStatus(taskId).orElseThrow();
    while (status.state() != TaskState.DONE) {
      if (status.state() == TaskState.FAILED || System.nanoTime() > deadline) {
        fail("Change was not propagated: " + status);
      }
      Thread.yield();
      status = worker.getStatus(taskId).orElseThrow();
    }
  }
}
