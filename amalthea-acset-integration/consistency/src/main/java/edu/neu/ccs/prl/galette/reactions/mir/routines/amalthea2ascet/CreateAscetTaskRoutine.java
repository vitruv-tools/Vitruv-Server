package edu.neu.ccs.prl.galette.reactions.mir.routines.amalthea2ascet;

import java.io.IOException;
import org.eclipse.xtext.xbase.lib.Conversions;
import org.eclipse.xtext.xbase.lib.Extension;
import org.eclipse.xtext.xbase.lib.InputOutput;
import tools.vitruv.dsls.reactions.runtime.routines.AbstractRoutine;
import tools.vitruv.dsls.reactions.runtime.state.ReactionExecutionState;
import tools.vitruv.dsls.reactions.runtime.structure.CallHierarchyHaving;
import ecore.tools.vitruv.methodologisttemplate.model.amalthea.ComponentContainer;
import ecore.tools.vitruv.methodologisttemplate.model.amalthea.Task;

@SuppressWarnings("all")
public class CreateAscetTaskRoutine extends AbstractRoutine {
  private InputValues inputValues;

  public class InputValues {
    public final Task task;

    public final ComponentContainer container;

    public InputValues(final Task task, final ComponentContainer container) {
      this.task = task;
      this.container = container;
    }
  }

  private static class Update extends AbstractRoutine.Update {
    public Update(final ReactionExecutionState reactionExecutionState) {
      super(reactionExecutionState);
    }

    public void updateModels(final Task task, final ComponentContainer container, @Extension final Amalthea2ascetRoutinesFacade _routinesFacade) {
      InputOutput.<String>println("[Reaction] createAscetTask routine CALLED!");
      String _name = task.getName();
      String _plus = ("  - Task: " + _name);
      InputOutput.<String>println(_plus);
      InputOutput.<String>println(("  - Container: " + container));

      // Yes/no gate: should a corresponding ASCET task be created at all?
      final Boolean createCorresponding = this.executionState.getUserInteractor()
          .getConfirmationDialogBuilder()
          .message("A Task has been created. Create a corresponding ASCET Task?")
          .positiveButtonText("Yes")
          .negativeButtonText("No")
          .startInteraction();
      if (createCorresponding == null || !createCorresponding.booleanValue()) {
        InputOutput.<String>println("[Reaction] User declined ASCET task creation (no).");
        return;
      }

      final String userMsg = "Please decide which corresponding ASCET Task should be created.";
      final String initTaskOption = "Create InitTask";
      final String periodicTaskOption = "Create PeriodicTask";
      final String softwareTaskOption = "Create SoftwareTask";
      final String timeTableTaskOption = "Create TimeTableTask";
      final String doNothingOption = "Decide Later";
      final String[] options = { initTaskOption, periodicTaskOption, softwareTaskOption, timeTableTaskOption, doNothingOption };
      InputOutput.<String>println("[Reaction] About to call userInteractor.startInteraction()...");
      final Integer selected = this.executionState.getUserInteractor().getSingleSelectionDialogBuilder().message(userMsg).choices(((Iterable<String>)Conversions.doWrapArray(options))).startInteraction();
      if (selected != null) {
        switch (selected) {
          case 0:
            _routinesFacade.createInitTask(task, container);
            break;
          case 1:
            _routinesFacade.createPeriodicTask(task, container);
            break;
          case 2:
            _routinesFacade.createSoftwareTask(task, container);
            break;
          case 3:
            _routinesFacade.createTimeTableTask(task, container);
            break;
          case 4:
            break;
        }
      }
    }
  }

  public CreateAscetTaskRoutine(final Amalthea2ascetRoutinesFacade routinesFacade, final ReactionExecutionState reactionExecutionState, final CallHierarchyHaving calledBy, final Task task, final ComponentContainer container) {
    super(routinesFacade, reactionExecutionState, calledBy);
    this.inputValues = new InputValues(task, container);
  }

  protected boolean executeRoutine() throws IOException {
    if (getLogger().isTraceEnabled()) {
    	getLogger().trace("Called routine CreateAscetTaskRoutine with input:");
    	getLogger().trace("   inputValues.task: " + inputValues.task);
    	getLogger().trace("   inputValues.container: " + inputValues.container);
    }
    // This execution step is empty
    // This execution step is empty
    new edu.neu.ccs.prl.galette.reactions.mir.routines.amalthea2ascet.CreateAscetTaskRoutine.Update(getExecutionState()).updateModels(inputValues.task, inputValues.container, getRoutinesFacade());
    return true;
  }
}
