package edu.neu.ccs.prl.galette.reactions.mir.routines.amalthea2ascet;

import java.io.IOException;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.xbase.lib.Extension;
import org.eclipse.xtext.xbase.lib.InputOutput;
import tools.vitruv.dsls.reactions.runtime.routines.AbstractRoutine;
import tools.vitruv.dsls.reactions.runtime.state.ReactionExecutionState;
import tools.vitruv.dsls.reactions.runtime.structure.CallHierarchyHaving;
import edu.neu.ccs.prl.galette.reactions.mir.DoubleInputValidator;
import edu.neu.ccs.prl.galette.reactions.mir.PositiveDoubleValidator;
import ecore.tools.vitruv.methodologisttemplate.model.amalthea.ComponentContainer;
import ecore.tools.vitruv.methodologisttemplate.model.amalthea.Task;
import ecore.tools.vitruv.methodologisttemplate.model.ascet.AscetModule;
import ecore.tools.vitruv.methodologisttemplate.model.ascet.AscetTask;
import ecore.tools.vitruv.methodologisttemplate.model.ascet.PeriodicTask;

@SuppressWarnings("all")
public class CreatePeriodicTaskRoutine extends AbstractRoutine {
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
      	ecore.tools.vitruv.methodologisttemplate.model.ascet.PeriodicTask.class,
      	null, // correspondence precondition checker
      	null
      )) {
      	return null;
      }
      return new edu.neu.ccs.prl.galette.reactions.mir.routines.amalthea2ascet.CreatePeriodicTaskRoutine.Match.RetrievedValues(AscetModule);
    }
  }

  private static class Create extends AbstractRoutine.Create {
    public class CreatedValues {
      public final PeriodicTask periodicTask;

      public CreatedValues(final PeriodicTask periodicTask) {
        this.periodicTask = periodicTask;
      }
    }

    public Create(final ReactionExecutionState reactionExecutionState) {
      super(reactionExecutionState);
    }

    public CreatedValues createElements() {
      ecore.tools.vitruv.methodologisttemplate.model.ascet.PeriodicTask periodicTask = createObject(() -> {
      	return ecore.tools.vitruv.methodologisttemplate.model.ascet.impl.AscetFactoryImpl.eINSTANCE.createPeriodicTask();
      });
      return new CreatedValues(periodicTask);
    }
  }

  private static class Update extends AbstractRoutine.Update {
    public Update(final ReactionExecutionState reactionExecutionState) {
      super(reactionExecutionState);
    }

    public void updateModels(final Task task, final ComponentContainer container, final AscetModule AscetModule, final PeriodicTask periodicTask, @Extension final Amalthea2ascetRoutinesFacade _routinesFacade) {
      EList<AscetTask> _tasks = AscetModule.getTasks();
      _tasks.add(periodicTask);
      periodicTask.setName(task.getName());
      final String periodMessage = "Please enter the period of the new task (positive double)";
      final String delayMessage = "Please enter the delay of the new task (positive double)";
      final Double period = Double.valueOf(Double.parseDouble(
          this.executionState.getUserInteractor().getTextInputDialogBuilder().message(periodMessage).inputValidator(PositiveDoubleValidator.getPositiveDoubleValidatorInstance()).startInteraction()));
      periodicTask.setPeriod((period).doubleValue());
      final Double delay = Double.valueOf(Double.parseDouble(
          this.executionState.getUserInteractor().getTextInputDialogBuilder().message(delayMessage).inputValidator(PositiveDoubleValidator.getPositiveDoubleValidatorInstance()).startInteraction()));
      periodicTask.setDelay((delay).doubleValue());

      this.addCorrespondenceBetween(periodicTask, container);
    }
  }

  public CreatePeriodicTaskRoutine(final Amalthea2ascetRoutinesFacade routinesFacade, final ReactionExecutionState reactionExecutionState, final CallHierarchyHaving calledBy, final Task task, final ComponentContainer container) {
    super(routinesFacade, reactionExecutionState, calledBy);
    this.inputValues = new InputValues(task, container);
  }

  protected boolean executeRoutine() throws IOException {
    if (getLogger().isTraceEnabled()) {
    	getLogger().trace("Called routine CreatePeriodicTaskRoutine with input:");
    	getLogger().trace("   inputValues.task: " + inputValues.task);
    	getLogger().trace("   inputValues.container: " + inputValues.container);
    }
    retrievedValues = new edu.neu.ccs.prl.galette.reactions.mir.routines.amalthea2ascet.CreatePeriodicTaskRoutine.Match(getExecutionState()).match(inputValues.task, inputValues.container);
    if (retrievedValues == null) {
    	return false;
    }
    createdValues = new edu.neu.ccs.prl.galette.reactions.mir.routines.amalthea2ascet.CreatePeriodicTaskRoutine.Create(getExecutionState()).createElements();
    new edu.neu.ccs.prl.galette.reactions.mir.routines.amalthea2ascet.CreatePeriodicTaskRoutine.Update(getExecutionState()).updateModels(inputValues.task, inputValues.container, retrievedValues.AscetModule, createdValues.periodicTask, getRoutinesFacade());
    return true;
  }
}
