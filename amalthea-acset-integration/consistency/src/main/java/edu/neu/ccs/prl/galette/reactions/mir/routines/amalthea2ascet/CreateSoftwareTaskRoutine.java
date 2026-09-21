package edu.neu.ccs.prl.galette.reactions.mir.routines.amalthea2ascet;

import java.io.IOException;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.xbase.lib.Extension;
import tools.vitruv.dsls.reactions.runtime.routines.AbstractRoutine;
import tools.vitruv.dsls.reactions.runtime.state.ReactionExecutionState;
import tools.vitruv.dsls.reactions.runtime.structure.CallHierarchyHaving;
import ecore.tools.vitruv.methodologisttemplate.model.amalthea.ComponentContainer;
import ecore.tools.vitruv.methodologisttemplate.model.amalthea.Task;
import ecore.tools.vitruv.methodologisttemplate.model.ascet.AscetModule;
import ecore.tools.vitruv.methodologisttemplate.model.ascet.AscetTask;
import ecore.tools.vitruv.methodologisttemplate.model.ascet.SoftwareTask;

@SuppressWarnings("all")
public class CreateSoftwareTaskRoutine extends AbstractRoutine {
  private InputValues inputValues;

  private Match.RetrievedValues retrievedValues;

  private Create.CreatedValues createdValues;

  public class InputValues {
    public final Task task;

    public final ComponentContainer container;

    public InputValues(final Task task, final ComponentContainer container) {
      this.task = task;
      this.container = container;
    }
  }

  private static class Match extends AbstractRoutine.Match {
    public class RetrievedValues {
      public final AscetModule AscetModule;

      public RetrievedValues(final AscetModule AscetModule) {
        this.AscetModule = AscetModule;
      }
    }

    public Match(final ReactionExecutionState reactionExecutionState) {
      super(reactionExecutionState);
    }

    public EObject getCorrepondenceSourceAscetModule(final Task task, final ComponentContainer container) {
      return container;
    }

    public EObject getCorrepondenceSource1(final Task task, final ComponentContainer container, final AscetModule AscetModule) {
      return task;
    }

    public RetrievedValues match(final Task task, final ComponentContainer container) throws IOException {
      ecore.tools.vitruv.methodologisttemplate.model.ascet.AscetModule AscetModule = getCorrespondingElement(
      	getCorrepondenceSourceAscetModule(task, container), // correspondence source supplier
          ecore.tools.vitruv.methodologisttemplate.model.ascet.AscetModule.class,
      	null, // correspondence precondition checker
      	null, 
      	false // asserted
      );
      if (AscetModule == null) {
      	return null;
      }
      if (hasCorrespondingElements(
      	getCorrepondenceSource1(task, container, AscetModule), // correspondence source supplier
          ecore.tools.vitruv.methodologisttemplate.model.ascet.SoftwareTask.class,
      	null, // correspondence precondition checker
      	null
      )) {
      	return null;
      }
      return new edu.neu.ccs.prl.galette.reactions.mir.routines.amalthea2ascet.CreateSoftwareTaskRoutine.Match.RetrievedValues(AscetModule);
    }
  }

  private static class Create extends AbstractRoutine.Create {
    public class CreatedValues {
      public final SoftwareTask softwareTask;

      public CreatedValues(final SoftwareTask softwareTask) {
        this.softwareTask = softwareTask;
      }
    }

    public Create(final ReactionExecutionState reactionExecutionState) {
      super(reactionExecutionState);
    }

    public CreatedValues createElements() {
      ecore.tools.vitruv.methodologisttemplate.model.ascet.SoftwareTask softwareTask = createObject(() -> {
      	return ecore.tools.vitruv.methodologisttemplate.model.ascet.impl.AscetFactoryImpl.eINSTANCE.createSoftwareTask();
      });
      return new CreatedValues(softwareTask);
    }
  }

  private static class Update extends AbstractRoutine.Update {
    public Update(final ReactionExecutionState reactionExecutionState) {
      super(reactionExecutionState);
    }

    public void updateModels(final Task task, final ComponentContainer container, final AscetModule AscetModule, final SoftwareTask softwareTask, @Extension final Amalthea2ascetRoutinesFacade _routinesFacade) {
      EList<AscetTask> _tasks = AscetModule.getTasks();
      _tasks.add(softwareTask);
      softwareTask.setName(task.getName());
      this.addCorrespondenceBetween(softwareTask, container);
    }
  }

  public CreateSoftwareTaskRoutine(final Amalthea2ascetRoutinesFacade routinesFacade, final ReactionExecutionState reactionExecutionState, final CallHierarchyHaving calledBy, final Task task, final ComponentContainer container) {
    super(routinesFacade, reactionExecutionState, calledBy);
    this.inputValues = new InputValues(task, container);
  }

  protected boolean executeRoutine() throws IOException {
    if (getLogger().isTraceEnabled()) {
    	getLogger().trace("Called routine CreateSoftwareTaskRoutine with input:");
    	getLogger().trace("   inputValues.task: " + inputValues.task);
    	getLogger().trace("   inputValues.container: " + inputValues.container);
    }
    retrievedValues = new edu.neu.ccs.prl.galette.reactions.mir.routines.amalthea2ascet.CreateSoftwareTaskRoutine.Match(getExecutionState()).match(inputValues.task, inputValues.container);
    if (retrievedValues == null) {
    	return false;
    }
    createdValues = new edu.neu.ccs.prl.galette.reactions.mir.routines.amalthea2ascet.CreateSoftwareTaskRoutine.Create(getExecutionState()).createElements();
    new edu.neu.ccs.prl.galette.reactions.mir.routines.amalthea2ascet.CreateSoftwareTaskRoutine.Update(getExecutionState()).updateModels(inputValues.task, inputValues.container, retrievedValues.AscetModule, createdValues.softwareTask, getRoutinesFacade());
    return true;
  }
}
