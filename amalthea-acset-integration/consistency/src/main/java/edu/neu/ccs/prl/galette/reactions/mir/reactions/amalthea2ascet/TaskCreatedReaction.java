package edu.neu.ccs.prl.galette.reactions.mir.reactions.amalthea2ascet;

import java.util.function.Function;
import edu.neu.ccs.prl.galette.reactions.mir.routines.amalthea2ascet.Amalthea2ascetRoutinesFacade;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtext.xbase.lib.Extension;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.feature.reference.InsertEReference;
import tools.vitruv.dsls.reactions.runtime.reactions.AbstractReaction;
import tools.vitruv.dsls.reactions.runtime.routines.AbstractRoutine;
import tools.vitruv.dsls.reactions.runtime.routines.RoutinesFacade;
import tools.vitruv.dsls.reactions.runtime.state.ReactionExecutionState;
import ecore.tools.vitruv.methodologisttemplate.model.amalthea.ComponentContainer;
import ecore.tools.vitruv.methodologisttemplate.model.amalthea.Task;

@SuppressWarnings("all")
public class TaskCreatedReaction extends AbstractReaction {
  private InsertEReference<EObject> insertChange;

  public TaskCreatedReaction(final Function<ReactionExecutionState, RoutinesFacade> routinesFacadeGenerator) {
    super(routinesFacadeGenerator);
  }

  private static class Call extends AbstractRoutine.Update {
    public Call(final ReactionExecutionState reactionExecutionState) {
      super(reactionExecutionState);
    }

    public void updateModels(final InsertEReference insertChange, final ComponentContainer affectedEObject, final EReference affectedFeature, final Task newValue, final int index, @Extension final Amalthea2ascetRoutinesFacade _routinesFacade) {
      _routinesFacade.createAscetTask(newValue, affectedEObject);
    }
  }

  public boolean isCurrentChangeMatchingTrigger(final EChange change) {
    if (!(change instanceof InsertEReference<?>)) {
    	return false;
    }
    
    InsertEReference<EObject> _localTypedChange = (InsertEReference<EObject>) change;
    if (!(_localTypedChange.getAffectedElement() instanceof ecore.tools.vitruv.methodologisttemplate.model.amalthea.ComponentContainer)) {
    	return false;
    }
    if (!_localTypedChange.getAffectedFeature().getName().equals("tasks")) {
    	return false;
    }
    if (!(_localTypedChange.getNewValue() instanceof ecore.tools.vitruv.methodologisttemplate.model.amalthea.Task)) {
    	return false;
    }
    this.insertChange = (InsertEReference<EObject>) change;
    return true;
  }

  public void executeReaction(final EChange change, final ReactionExecutionState executionState, final RoutinesFacade routinesFacadeUntyped) {
    edu.neu.ccs.prl.galette.reactions.mir.routines.amalthea2ascet.Amalthea2ascetRoutinesFacade routinesFacade = (edu.neu.ccs.prl.galette.reactions.mir.routines.amalthea2ascet.Amalthea2ascetRoutinesFacade)routinesFacadeUntyped;
    if (!isCurrentChangeMatchingTrigger(change)) {
    	return;
    }
    ecore.tools.vitruv.methodologisttemplate.model.amalthea.ComponentContainer affectedEObject = (ecore.tools.vitruv.methodologisttemplate.model.amalthea.ComponentContainer)insertChange.getAffectedElement();
    EReference affectedFeature = insertChange.getAffectedFeature();
    ecore.tools.vitruv.methodologisttemplate.model.amalthea.Task newValue = (ecore.tools.vitruv.methodologisttemplate.model.amalthea.Task)insertChange.getNewValue();
    int index = insertChange.getIndex();
    if (getLogger().isTraceEnabled()) {
    	getLogger().trace("Passed complete precondition check of Reaction " + this.getClass().getName());
    }
    
    new edu.neu.ccs.prl.galette.reactions.mir.reactions.amalthea2ascet.TaskCreatedReaction.Call(executionState).updateModels(insertChange, affectedEObject, affectedFeature, newValue, index, routinesFacade);
  }
}
