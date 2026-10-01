package tools.vitruv.methodologisttemplate.vsum;

import vitruv.tools.framework.remote.vsumprovider.VirtualModelInitializer;
import vitruv.tools.framework.remote.vsumprovider.VsumProvider;
import edu.neu.ccs.prl.galette.reactions.mir.reactions.amalthea2ascet.Amalthea2ascetChangePropagationSpecification;
import ecore.tools.vitruv.methodologisttemplate.model.amalthea.AmaltheaFactory;
import ecore.tools.vitruv.methodologisttemplate.model.amalthea.AmaltheaPackage;
import ecore.tools.vitruv.methodologisttemplate.model.amalthea.ComponentContainer;
import ecore.tools.vitruv.methodologisttemplate.model.ascet.AscetFactory;
import ecore.tools.vitruv.methodologisttemplate.model.ascet.AscetModule;
import ecore.tools.vitruv.methodologisttemplate.model.ascet.AscetPackage;
import ecore.tools.vitruv.methodologisttemplate.model.ascet.AscetTask;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import tools.vitruv.change.interaction.InteractionResultProvider;
import tools.vitruv.change.propagation.ChangePropagationMode;
import tools.vitruv.change.testutils.TestUserInteraction;
import tools.vitruv.change.utils.ProjectMarker;
import tools.vitruv.framework.views.CommittableView;
import tools.vitruv.framework.views.View;
import tools.vitruv.framework.views.ViewTypeFactory;
import tools.vitruv.framework.vsum.VirtualModel;
import tools.vitruv.framework.vsum.VirtualModelBuilder;
import tools.vitruv.framework.vsum.internal.InternalVirtualModel;

/**
 * VsumProvider for the Amalthea → ASCET case study, loadable by VitruviusServer.
 *
 * <p>With async propagation, VitruviusServer injects {@link InteractionResultProvider} so Task
 * create reactions pause for confirmation, ASCET task kind, and PeriodicTask period/delay in the
 * web UI. The no-arg initializer keeps auto-answers for local/sync fallback.
 *
 * <p>Like SystemRoot, {@code persistProjectRelative} can write {@code example.model2} next to the
 * server cwd; {@link #fixAscetModuleInStorage} relocates it into the per-VSUM storage folder.
 */
public class AmaltheaAscetVsumProvider implements VsumProvider {

    public static final String METAMODEL_NAME = "AmaltheaAscet";

    /** Index of "Create SoftwareTask" in createAscetTask options (sync/local auto-answer). */
    private static final int DEFAULT_SOFTWARE_TASK_CHOICE = 2;

    @Override
    public String getMetamodelName() {
        return METAMODEL_NAME;
    }

    @Override
    public VirtualModelInitializer getInitializer(Path storagePath) {
        return getInitializer(storagePath, new TestUserInteraction.ResultProvider(createDefaultUserInteraction()));
    }

    @Override
    public VirtualModelInitializer getInitializer(
            Path storagePath, InteractionResultProvider interactionResultProvider) {
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());
        AmaltheaPackage.eINSTANCE.eClass();
        AscetPackage.eINSTANCE.eClass();

        return () -> {
            InternalVirtualModel vsum = createDefaultVirtualModel(storagePath, interactionResultProvider);
            if (!Files.exists(storagePath.resolve("example.model"))) {
                addComponentContainer(vsum, storagePath);
            }
            fixAscetModuleInStorage(vsum, storagePath);
            ensureContainerModuleCorrespondence(vsum);
            return vsum;
        };
    }

    private static InternalVirtualModel createDefaultVirtualModel(
            Path projectPath, InteractionResultProvider interactionResultProvider) {
        try {
            Files.createDirectories(projectPath);
            // Recovered / reopened VSUMs already have the marker; createFile would fail.
            if (!Files.exists(projectPath.resolve("test_project.marker_vitruv"))) {
                ProjectMarker.markAsProjectRootFolder(projectPath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to prepare VSUM storage folder: " + projectPath, e);
        }

        // Ensure reactions correspondence EPackage is registered before loading existing storage.
        tools.vitruv.dsls.reactions.runtime.correspondence.CorrespondencePackage.eINSTANCE.eClass();
        tools.vitruv.change.correspondence.CorrespondencePackage.eINSTANCE.eClass();

        InternalVirtualModel model;
        try {
            model = new VirtualModelBuilder()
                    .withStorageFolder(projectPath)
                    .withUserInteractorForResultProvider(interactionResultProvider)
                    .withChangePropagationSpecifications(new Amalthea2ascetChangePropagationSpecification())
                    .withViewType(ViewTypeFactory.createIdentityMappingViewType("default", AmaltheaPackage.eINSTANCE))
                    .buildAndInitialize();
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize VSUM at " + projectPath, e);
        }
        model.setChangePropagationMode(ChangePropagationMode.TRANSITIVE_CYCLIC);
        return model;
    }

    /**
     * Auto-answers for sync/local fallback only. Interactive path uses the server-injected
     * provider (async Update) or CLI {@link Test}.
     */
    private static TestUserInteraction createDefaultUserInteraction() {
        TestUserInteraction user = new TestUserInteraction();
        user.onConfirmation(description -> true).always().respondWith(true);
        user.onMultipleChoiceSingleSelection(description -> true)
                .always()
                .respondWithChoiceAt(DEFAULT_SOFTWARE_TASK_CHOICE);
        user.onTextInput(description -> true).always().respondWith("1.0");
        return user;
    }

    private static View getDefaultView(VirtualModel vsum, Collection<Class<?>> rootTypes) {
        var selector = vsum.createSelector(
                ViewTypeFactory.createIdentityMappingViewType("default", AmaltheaPackage.eINSTANCE));
        selector.getSelectableElements().stream()
                .filter(element -> rootTypes.stream().anyMatch(it -> it.isInstance(element)))
                .forEach(it -> selector.setSelected(it, true));
        return selector.createView();
    }

    private static void modifyView(CommittableView view, Consumer<CommittableView> modificationFunction) {
        modificationFunction.accept(view);
        view.commitChanges();
    }

    private static void addComponentContainer(VirtualModel vsum, Path projectPath) {
        String base = projectPath.toAbsolutePath().toString().replace('\\', '/');
        URI containerUri = URI.createFileURI(base + "/example.model");

        CommittableView view = getDefaultView(vsum, List.of(ComponentContainer.class)).withChangeDerivingTrait();
        modifyView(view, v -> v.registerRoot(AmaltheaFactory.eINSTANCE.createComponentContainer(), containerUri));
    }

    /**
     * Relocates AscetModule from a misplaced URI (e.g. VitruviusServer/example.model2) into
     * {@code projectPath/example.model2}, preserving tasks and ComponentContainer↔AscetModule
     * correspondences. Same workaround as SystemRootVsumProvider.fixRootInStorage.
     */
    private static void fixAscetModuleInStorage(InternalVirtualModel vsum, Path projectPath) {
        ComponentContainer container = null;
        AscetModule module = null;

        var selector = vsum.createSelector(
                ViewTypeFactory.createIdentityMappingViewType("default", AmaltheaPackage.eINSTANCE));
        for (EObject element : selector.getSelectableElements()) {
            if (element instanceof ComponentContainer c) {
                container = c;
            } else if (element instanceof AscetModule m) {
                module = m;
            }
        }

        if (container == null || module == null || module.eResource() == null) {
            return;
        }

        String base = projectPath.toAbsolutePath().toString().replace('\\', '/');
        URI expectedUri = URI.createFileURI(base + "/example.model2");
        URI actualUri = module.eResource().getURI();

        if (normalizeUri(actualUri).equals(normalizeUri(expectedUri))) {
            return;
        }

        AscetModule relocated = AscetFactory.eINSTANCE.createAscetModule();
        relocated.setName(module.getName());
        for (AscetTask task : EcoreUtil.copyAll(module.getTasks())) {
            relocated.getTasks().add(task);
        }

        var correspondenceModel = vsum.getCorrespondenceModel();
        correspondenceModel.removeCorrespondencesBetween(container, module, "");
        correspondenceModel.removeCorrespondencesBetween(module, container, "");

        var misplaced = vsum.getModelInstance(actualUri);
        if (misplaced != null) {
            misplaced.delete();
        }

        CommittableView view = getDefaultView(vsum, List.of(AscetModule.class)).withChangeRecordingTrait();
        modifyView(view, v -> v.registerRoot(relocated, expectedUri));

        correspondenceModel.addCorrespondenceBetween(container, relocated, "");
        correspondenceModel.addCorrespondenceBetween(relocated, container, "");
    }

    private static void ensureContainerModuleCorrespondence(InternalVirtualModel vsum) {
        ComponentContainer container = null;
        AscetModule module = null;

        var selector = vsum.createSelector(
                ViewTypeFactory.createIdentityMappingViewType("default", AmaltheaPackage.eINSTANCE));
        for (EObject element : selector.getSelectableElements()) {
            if (element instanceof ComponentContainer c) {
                container = c;
            } else if (element instanceof AscetModule m) {
                module = m;
            }
        }

        if (container == null || module == null) {
            return;
        }

        var correspondenceModel = vsum.getCorrespondenceModel();
        boolean moduleHasContainer = correspondenceModel.getCorrespondingEObjects(module).stream()
                .anyMatch(ComponentContainer.class::isInstance);
        if (!moduleHasContainer) {
            correspondenceModel.addCorrespondenceBetween(container, module, "");
            correspondenceModel.addCorrespondenceBetween(module, container, "");
        }
    }

    private static String normalizeUri(URI uri) {
        return uri.toString().replace('\\', '/');
    }
}
