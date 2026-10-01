package tools.vitruv.methodologisttemplate.vsum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.neu.ccs.prl.galette.reactions.mir.reactions.amalthea2ascet.Amalthea2ascetChangePropagationSpecification;
import ecore.tools.vitruv.methodologisttemplate.model.amalthea.AmaltheaFactory;
import ecore.tools.vitruv.methodologisttemplate.model.amalthea.ComponentContainer;
import ecore.tools.vitruv.methodologisttemplate.model.amalthea.Task;
import ecore.tools.vitruv.methodologisttemplate.model.ascet.AscetModule;
import ecore.tools.vitruv.methodologisttemplate.model.ascet.AscetTask;
import ecore.tools.vitruv.methodologisttemplate.model.ascet.PeriodicTask;
import ecore.tools.vitruv.methodologisttemplate.model.ascet.SoftwareTask;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.vitruv.change.propagation.ChangePropagationMode;
import tools.vitruv.change.testutils.TestUserInteraction;
import tools.vitruv.framework.views.CommittableView;
import tools.vitruv.framework.views.View;
import tools.vitruv.framework.views.ViewTypeFactory;
import tools.vitruv.framework.vsum.VirtualModel;
import tools.vitruv.framework.vsum.VirtualModelBuilder;
import tools.vitruv.framework.vsum.internal.InternalVirtualModel;

/**
 * Tests user-input and delete propagation for the Amalthea → ASCET case study
 * (CoCoPath / UserInteractionDemo), using scripted {@link TestUserInteraction}.
 *
 * <p>When an Amalthea {@link Task} is inserted, the reaction asks for confirmation
 * (yes/no). Only on "yes" does it create a corresponding ASCET task (after a type
 * selection, and for PeriodicTask also period/delay text input).
 *
 * <p>Delete: removing an Amalthea Task from {@link ComponentContainer#getTasks()}
 * removes the ASCET task with the same name under the corresponding {@link AscetModule}
 * (create routines pair ASCET tasks with the container; delete resolves via that root link).
 */
class UserInputPropagationTest {

    private static final int SELECT_SOFTWARE_TASK = 2;
    private static final int SELECT_PERIODIC_TASK = 1;

    @BeforeAll
    static void setupEmf() {
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());
    }

    @Test
    void noConfirmation_doesNotCreateAscetTask(@TempDir Path tempDir) {
        TestUserInteraction user = new TestUserInteraction();
        user.addNextConfirmationInput(false); // No

        InternalVirtualModel vsum = createVsum(tempDir, user);
        addComponentContainer(vsum, tempDir);
        addTask(vsum, "task-declined");

        assertEquals(1, getDefaultView(vsum, List.of(ComponentContainer.class))
                .getRootObjects(ComponentContainer.class)
                .size());
        assertEquals(
                1,
                getDefaultView(vsum, List.of(AscetModule.class)).getRootObjects(AscetModule.class).size(),
                "root AscetModule is still created from ComponentContainer");
        assertEquals(0, ascetTasks(vsum).size(), "no ASCET task when user answers no");
        user.assertAllInteractionsOccurred();
    }

    @Test
    void yesConfirmation_createsSoftwareTask(@TempDir Path tempDir) {
        TestUserInteraction user = new TestUserInteraction();
        user.addNextConfirmationInput(true); // Yes
        user.addNextSingleSelection(SELECT_SOFTWARE_TASK);

        InternalVirtualModel vsum = createVsum(tempDir, user);
        addComponentContainer(vsum, tempDir);
        addTask(vsum, "engine-control");

        List<AscetTask> tasks = ascetTasks(vsum);
        assertEquals(1, tasks.size());
        assertTrue(tasks.get(0) instanceof SoftwareTask);
        assertEquals("engine-control", tasks.get(0).getName());
        user.assertAllInteractionsOccurred();
    }

    @Test
    void yesConfirmation_createsPeriodicTaskWithPeriodAndDelay(@TempDir Path tempDir) {
        TestUserInteraction user = new TestUserInteraction();
        user.addNextConfirmationInput(true); // Yes
        user.addNextSingleSelection(SELECT_PERIODIC_TASK);
        user.addNextTextInput("10.5"); // period
        user.addNextTextInput("1.0"); // delay

        InternalVirtualModel vsum = createVsum(tempDir, user);
        addComponentContainer(vsum, tempDir);
        addTask(vsum, "periodic-sensor");

        List<AscetTask> tasks = ascetTasks(vsum);
        assertEquals(1, tasks.size());
        assertTrue(tasks.get(0) instanceof PeriodicTask);
        PeriodicTask periodic = (PeriodicTask) tasks.get(0);
        assertEquals("periodic-sensor", periodic.getName());
        assertEquals(10.5, periodic.getPeriod(), 1e-9);
        assertEquals(1.0, periodic.getDelay(), 1e-9);
        user.assertAllInteractionsOccurred();
    }

    @Test
    void deletingAmaltheaTask_removesCorrespondingAscetTask(@TempDir Path tempDir) {
        TestUserInteraction user = new TestUserInteraction();
        user.addNextConfirmationInput(true);
        user.addNextSingleSelection(SELECT_SOFTWARE_TASK);

        InternalVirtualModel vsum = createVsum(tempDir, user);
        addComponentContainer(vsum, tempDir);
        addTask(vsum, "to-delete");
        assertEquals(1, ascetTasks(vsum).size());

        deleteTaskNamed(vsum, "to-delete");

        assertEquals(0, ascetTasks(vsum).size(), "ASCET task must be removed when Amalthea Task is deleted");
        user.assertAllInteractionsOccurred();
    }

    private static InternalVirtualModel createVsum(Path projectDir, TestUserInteraction user) {
        InternalVirtualModel vsum;
        try {
            vsum = new VirtualModelBuilder()
                    .withStorageFolder(projectDir)
                    .withUserInteractorForResultProvider(new TestUserInteraction.ResultProvider(user))
                    .withChangePropagationSpecifications(new Amalthea2ascetChangePropagationSpecification())
                    .buildAndInitialize();
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize VSUM at " + projectDir, e);
        }
        vsum.setChangePropagationMode(ChangePropagationMode.TRANSITIVE_CYCLIC);
        return vsum;
    }

    private static void addComponentContainer(VirtualModel vsum, Path projectDir) {
        CommittableView view = getDefaultView(vsum, List.of(ComponentContainer.class)).withChangeDerivingTrait();
        modifyView(view, v -> v.registerRoot(
                AmaltheaFactory.eINSTANCE.createComponentContainer(),
                URI.createFileURI(projectDir.resolve("example.model").toString())));
    }

    private static void addTask(VirtualModel vsum, String taskName) {
        CommittableView view = getDefaultView(vsum, List.of(ComponentContainer.class)).withChangeDerivingTrait();
        modifyView(view, v -> {
            Task task = AmaltheaFactory.eINSTANCE.createTask();
            task.setName(taskName);
            v.getRootObjects(ComponentContainer.class).iterator().next().getTasks().add(task);
        });
    }

    private static void deleteTaskNamed(VirtualModel vsum, String taskName) {
        CommittableView view = getDefaultView(vsum, List.of(ComponentContainer.class)).withChangeDerivingTrait();
        modifyView(view, v -> {
            ComponentContainer container = v.getRootObjects(ComponentContainer.class).iterator().next();
            Task toRemove = container.getTasks().stream()
                    .filter(t -> taskName.equals(t.getName()))
                    .findFirst()
                    .orElseThrow();
            container.getTasks().remove(toRemove);
        });
    }

    private static View getDefaultView(VirtualModel vsum, Collection<Class<?>> rootTypes) {
        var selector = vsum.createSelector(ViewTypeFactory.createIdentityMappingViewType("default"));
        selector.getSelectableElements().stream()
                .filter(e -> rootTypes.stream().anyMatch(t -> t.isInstance(e)))
                .forEach(e -> selector.setSelected(e, true));
        return selector.createView();
    }

    private static void modifyView(CommittableView view, Consumer<CommittableView> change) {
        change.accept(view);
        view.commitChanges();
    }

    private static List<AscetTask> ascetTasks(VirtualModel vsum) {
        View view = getDefaultView(vsum, List.of(AscetModule.class));
        AscetModule module = view.getRootObjects(AscetModule.class).iterator().next();
        List<AscetTask> tasks = List.copyOf(module.getTasks());
        try {
            view.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return tasks;
    }
}
