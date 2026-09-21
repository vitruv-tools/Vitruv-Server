package edu.neu.ccs.prl.galette.reactions.mir.reactions.amalthea2ascet;

import java.util.function.Function;
import edu.neu.ccs.prl.galette.reactions.mir.routines.amalthea2ascet.Amalthea2ascetRoutinesFacade;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtext.xbase.lib.Extension;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.feature.reference.RemoveEReference;
import tools.vitruv.dsls.reactions.runtime.reactions.AbstractReaction;
import tools.vitruv.dsls.reactions.runtime.routines.AbstractRoutine;
import tools.vitruv.dsls.reactions.runtime.routines.RoutinesFacade;
import tools.vitruv.dsls.reactions.runtime.state.ReactionExecutionState;
import ecore.tools.vitruv.methodologisttemplate.model.amalthea.ComponentContainer;
import ecore.tools.vitruv.methodologisttemplate.model.amalthea.Task;

@SuppressWarnings("all")
public class TaskDeletedReaction extends AbstractReaction {
  private RemoveEReference<EObject> removeChange;

  public TaskDeletedReaction(final Function<ReactionExecutionState, RoutinesFacade> routinesFacadeGenerator) {
    super(routinesFacadeGenerator);
  }

  private static class Call extends AbstractRoutine.Update {
    public Call(final ReactionExecutionState reactionExecutionState) {
      super(reactionExecutionState);
    }

    public void updateModels(
        final RemoveEReference removeChange,
        final ComponentContainer affectedEObject,
        final EReference affectedFeature,
        final Task oldValue,
        final int index,
        @Extension final Amalthea2ascetRoutinesFacade _routinesFacade) {
      _routinesFacade.deleteTask(oldValue, affectedEObject);
    }
  }

  public boolean isCurrentChangeMatchingTrigger(final EChange change) {
    if (!(change instanceof RemoveEReference<?>)) {
      return false;
    }

    RemoveEReference<EObject> _localTypedChange = (RemoveEReference<EObject>) change;
    if (!(_localTypedChange.getAffectedElement() instanceof ComponentContainer)) {
      return false;
    }
    if (!_localTypedChange.getAffectedFeature().getName().equals("tasks")) {
      return false;
    }
    if (!(_localTypedChange.getOldValue() instanceof Task)) {
      return false;
    }
    this.removeChange = (RemoveEReference<EObject>) change;
    return true;
  }

  public void executeReaction(final EChange change, final ReactionExecutionState executionState, final RoutinesFacade routinesFacadeUntyped) {
    Amalthea2ascetRoutinesFacade routinesFacade = (Amalthea2ascetRoutinesFacade) routinesFacadeUntyped;
    if (!isCurrentChangeMatchingTrigger(change)) {
      return;
    }
    ComponentContainer affectedEObject = (ComponentContainer) removeChange.getAffectedElement();
    EReference affectedFeature = removeChange.getAffectedFeature();
    Task oldValue = (Task) removeChange.getOldValue();
    int index = removeChange.getIndex();
    if (getLogger().isTraceEnabled()) {
      getLogger().trace("Passed complete precondition check of Reaction " + this.getClass().getName());
    }

    new Call(executionState).updateModels(removeChange, affectedEObject, affectedFeature, oldValue, index, routinesFacade);
  }
}
