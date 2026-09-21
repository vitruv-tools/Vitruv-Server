package tools.vitruv.methodologisttemplate.vsum;

import ecore.tools.vitruv.methodologisttemplate.model.amalthea.AmaltheaFactory;
import ecore.tools.vitruv.methodologisttemplate.model.amalthea.ComponentContainer;
import ecore.tools.vitruv.methodologisttemplate.model.amalthea.Task;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import edu.neu.ccs.prl.galette.reactions.mir.reactions.amalthea2ascet.Amalthea2ascetChangePropagationSpecification;
import tools.vitruv.change.propagation.ChangePropagationMode;
import tools.vitruv.framework.views.CommittableView;
import tools.vitruv.framework.views.View;
import tools.vitruv.framework.views.ViewTypeFactory;
import tools.vitruv.framework.vsum.VirtualModel;
import tools.vitruv.framework.vsum.VirtualModelBuilder;
import tools.vitruv.framework.vsum.internal.InternalVirtualModel;

public class Test {

    public static void main(String[] args) {

        insertTask(Path.of("./testdir"));
    }

    /**
     * Single-variable version: Insert one task with one user choice.
     * Used for single-variable path exploration (5 paths).
     */
    public static void insertTask(Path projectDir) {
        // 2)  build VSUM with user Interaction
        InternalVirtualModel vsum = new VirtualModelBuilder()
                .withStorageFolder(projectDir)
                .withUserInteractorForResultProvider(new CliInteractionResultProviderImpl())
                .withChangePropagationSpecifications(new Amalthea2ascetChangePropagationSpecification())
                .buildAndInitialize();

        vsum.setChangePropagationMode(ChangePropagationMode.TRANSITIVE_CYCLIC);

        // 3) Add task and
        // Register XMI resource factory (CRITICAL for EMF resource creation)
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());
        addComponentContainer(vsum, projectDir);
        addTask(vsum);

        // 4) merge and save
        try {
            Path outDir = projectDir.resolve("galette-test-output");
            mergeAndSave(vsum, outDir, "vsum-output.xmi");
        } catch (IOException e) {
            throw new RuntimeException("Could not persist VSUM result", e);
        }
    }

    /* helpers */

    private static void addComponentContainer(VirtualModel vsum, Path projectDir) {
        // Ensure the model file path exists as a directory for Vitruvius resource creation
        try {
            Files.createDirectories(projectDir);
        } catch (IOException e) {
            // Directory likely already exists, continue
        }

        CommittableView view = getDefaultView(vsum, Collections.singletonList(ComponentContainer.class))
                .withChangeDerivingTrait();
        modifyView(
                view,
                v -> v.registerRoot(
                    AmaltheaFactory.eINSTANCE.createComponentContainer(),
                        URI.createFileURI(projectDir.resolve("example.model").toString())));
    }

    private static void addTask(VirtualModel vsum) {
        addTaskWithName(vsum, "specialname");
    }

    private static void addTaskWithName(VirtualModel vsum, String taskName) {
        CommittableView view = getDefaultView(vsum, Collections.singletonList(ComponentContainer.class))
                .withChangeDerivingTrait();
        modifyView(view, v -> {
            Task task = AmaltheaFactory.eINSTANCE.createTask();
            task.setName(taskName);
            v.getRootObjects(ComponentContainer.class)
                    .iterator()
                    .next()
                    .getTasks()
                    .add(task);
        });
    }

    private static View getDefaultView(VirtualModel vsum, Collection<Class<?>> rootTypes) {
        tools.vitruv.framework.views.ViewSelector selector =
                vsum.createSelector(ViewTypeFactory.createIdentityMappingViewType("default"));
        selector.getSelectableElements().stream()
                .filter(e -> rootTypes.stream().anyMatch(t -> t.isInstance(e)))
                .forEach(e -> selector.setSelected(e, true));
        return selector.createView();
    }

    private static void modifyView(CommittableView view, Consumer<CommittableView> change) {
        change.accept(view);
        view.commitChanges();
    }

    private static void mergeAndSave(InternalVirtualModel vm, Path outDir, String fileName) throws IOException {
        Files.createDirectories(outDir);

        ResourceSet rs = new ResourceSetImpl();
        URI mergedUri = URI.createFileURI(outDir.resolve(fileName).toString());
        Resource merged = rs.createResource(mergedUri);

        for (Resource src : vm.getViewSourceModels()) {
            for (EObject obj : src.getContents()) {
                merged.getContents().add(EcoreUtil.copy(obj));
            }
        }

        Map<String, Object> opts = new HashMap<>();
        opts.put(XMLResource.OPTION_ENCODING, "UTF-8");
        opts.put(XMLResource.OPTION_FORMATTED, Boolean.TRUE);
        opts.put(XMLResource.OPTION_SCHEMA_LOCATION, Boolean.TRUE);
        merged.save(opts);
    }
}
