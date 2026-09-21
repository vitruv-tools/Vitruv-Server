package edu.neu.ccs.prl.galette.reactions.mir.routines.amalthea2ascet;

import java.io.IOException;
import java.util.Objects;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.xbase.lib.Extension;
import tools.vitruv.dsls.reactions.runtime.routines.AbstractRoutine;
import tools.vitruv.dsls.reactions.runtime.state.ReactionExecutionState;
import tools.vitruv.dsls.reactions.runtime.structure.CallHierarchyHaving;
import ecore.tools.vitruv.methodologisttemplate.model.amalthea.ComponentContainer;
import ecore.tools.vitruv.methodologisttemplate.model.amalthea.Task;
import ecore.tools.vitruv.methodologisttemplate.model.ascet.AscetModule;
import ecore.tools.vitruv.methodologisttemplate.model.ascet.AscetTask;

/**
 * Removes the ASCET task that mirrors an Amalthea {@link Task}.
 *
 * <p>Create routines follow the upstream pairing {@code AscetTask ↔ ComponentContainer}. Delete
 * therefore resolves {@link AscetModule} via the container and matches by task name (same pattern
 * the case study used for lookup through the container root link).
 */
@SuppressWarnings("all")
public class DeleteTaskRoutine extends AbstractRoutine {
  private InputValues inputValues;

  private Match.RetrievedValues retrievedValues;

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

    public RetrievedValues match(final Task task, final ComponentContainer container) throws IOException {
      AscetModule AscetModule = getCorrespondingElement(
          getCorrepondenceSourceAscetModule(task, container),
          AscetModule.class,
          null,
          null,
          false);
      if (AscetModule == null) {
        return null;
      }
      return new RetrievedValues(AscetModule);
    }
  }

  private static class Update extends AbstractRoutine.Update {
    public Update(final ReactionExecutionState reactionExecutionState) {
      super(reactionExecutionState);
    }

    public void updateModels(
        final Task task,
        final ComponentContainer container,
        final AscetModule AscetModule,
        @Extension final Amalthea2ascetRoutinesFacade _routinesFacade) {
      AscetTask ascettask = AscetModule.getTasks().stream()
          .filter(t -> Objects.equals(t.getName(), task.getName()))
          .findFirst()
          .orElse(null);
      if (ascettask != null) {
        this.removeObject(ascettask);
        this.removeCorrespondenceBetween(ascettask, container);
      }
    }
  }

  public DeleteTaskRoutine(
      final Amalthea2ascetRoutinesFacade routinesFacade,
      final ReactionExecutionState reactionExecutionState,
      final CallHierarchyHaving calledBy,
      final Task task,
      final ComponentContainer container) {
    super(routinesFacade, reactionExecutionState, calledBy);
    this.inputValues = new InputValues(task, container);
  }

  protected boolean executeRoutine() throws IOException {
    if (getLogger().isTraceEnabled()) {
      getLogger().trace("Called routine DeleteTaskRoutine with input:");
      getLogger().trace("   inputValues.task: " + inputValues.task);
      getLogger().trace("   inputValues.container: " + inputValues.container);
    }
    retrievedValues = new Match(getExecutionState()).match(inputValues.task, inputValues.container);
    if (retrievedValues == null) {
      return false;
    }
    new Update(getExecutionState())
        .updateModels(inputValues.task, inputValues.container, retrievedValues.AscetModule, getRoutinesFacade());
    return true;
  }
}
