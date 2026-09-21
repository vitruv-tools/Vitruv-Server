package edu.neu.ccs.prl.galette.reactions.mir.reactions.amalthea2ascet;

import java.util.function.Function;
import edu.neu.ccs.prl.galette.reactions.mir.routines.amalthea2ascet.Amalthea2ascetRoutinesFacade;
import org.eclipse.xtext.xbase.lib.Extension;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.root.InsertRootEObject;
import tools.vitruv.dsls.reactions.runtime.reactions.AbstractReaction;
import tools.vitruv.dsls.reactions.runtime.routines.AbstractRoutine;
import tools.vitruv.dsls.reactions.runtime.routines.RoutinesFacade;
import tools.vitruv.dsls.reactions.runtime.state.ReactionExecutionState;
import ecore.tools.vitruv.methodologisttemplate.model.amalthea.ComponentContainer;

@SuppressWarnings("all")
public class ComponentContainerInsertedAsRootReaction extends AbstractReaction {
  private InsertRootEObject<ComponentContainer> insertChange;

  public ComponentContainerInsertedAsRootReaction(final Function<ReactionExecutionState, RoutinesFacade> routinesFacadeGenerator) {
    super(routinesFacadeGenerator);
  }

  private static class Call extends AbstractRoutine.Update {
    public Call(final ReactionExecutionState reactionExecutionState) {
      super(reactionExecutionState);
    }

    public void updateModels(final InsertRootEObject insertChange, final ComponentContainer newValue, final int index, @Extension final Amalthea2ascetRoutinesFacade _routinesFacade) {
      _routinesFacade.createAndRegisterRootComponentContainer(newValue);
    }
  }

  public boolean isCurrentChangeMatchingTrigger(final EChange change) {
    if (!(change instanceof InsertRootEObject<?>)) {
    	return false;
    }
    
    InsertRootEObject<ecore.tools.vitruv.methodologisttemplate.model.amalthea.ComponentContainer> _localTypedChange = (InsertRootEObject<ecore.tools.vitruv.methodologisttemplate.model.amalthea.ComponentContainer>) change;
    if (!(_localTypedChange.getNewValue() instanceof ecore.tools.vitruv.methodologisttemplate.model.amalthea.ComponentContainer)) {
    	return false;
    }
    this.insertChange = (InsertRootEObject<ecore.tools.vitruv.methodologisttemplate.model.amalthea.ComponentContainer>) change;
    return true;
  }

  public void executeReaction(final EChange change, final ReactionExecutionState executionState, final RoutinesFacade routinesFacadeUntyped) {
    Amalthea2ascetRoutinesFacade routinesFacade = (Amalthea2ascetRoutinesFacade)routinesFacadeUntyped;
    if (!isCurrentChangeMatchingTrigger(change)) {
    	return;
    }
    ecore.tools.vitruv.methodologisttemplate.model.amalthea.ComponentContainer newValue = (ecore.tools.vitruv.methodologisttemplate.model.amalthea.ComponentContainer)insertChange.getNewValue();
    int index = insertChange.getIndex();
    if (getLogger().isTraceEnabled()) {
    	getLogger().trace("Passed complete precondition check of Reaction " + this.getClass().getName());
    }
    
    new edu.neu.ccs.prl.galette.reactions.mir.reactions.amalthea2ascet.ComponentContainerInsertedAsRootReaction.Call(executionState).updateModels(insertChange, newValue, index, routinesFacade);
  }
}
