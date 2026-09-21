package edu.neu.ccs.prl.galette.reactions.mir.routines.amalthea2ascet;

import tools.vitruv.dsls.reactions.runtime.routines.AbstractRoutinesFacade;
import tools.vitruv.dsls.reactions.runtime.routines.RoutinesFacadesProvider;
import tools.vitruv.dsls.reactions.runtime.state.ReactionExecutionState;
import tools.vitruv.dsls.reactions.runtime.structure.CallHierarchyHaving;
import tools.vitruv.dsls.reactions.runtime.structure.ReactionsImportPath;
import ecore.tools.vitruv.methodologisttemplate.model.amalthea.ComponentContainer;
import ecore.tools.vitruv.methodologisttemplate.model.amalthea.Task;

@SuppressWarnings("all")
public class Amalthea2ascetRoutinesFacade extends AbstractRoutinesFacade {
  public Amalthea2ascetRoutinesFacade(final RoutinesFacadesProvider routinesFacadesProvider, final ReactionsImportPath reactionsImportPath) {
    super(routinesFacadesProvider, reactionsImportPath);
  }

  public boolean createAndRegisterRootComponentContainer(final ComponentContainer componentContainer) {
    Amalthea2ascetRoutinesFacade _routinesFacade = this;
    ReactionExecutionState _executionState = _getExecutionState();
    CallHierarchyHaving _caller = this._getCurrentCaller();
    CreateAndRegisterRootComponentContainerRoutine routine = new CreateAndRegisterRootComponentContainerRoutine(_routinesFacade, _executionState, _caller, componentContainer);
    return routine.execute();
  }

  public boolean deleteTask(final Task task, final ComponentContainer container) {
    Amalthea2ascetRoutinesFacade _routinesFacade = this;
    ReactionExecutionState _executionState = _getExecutionState();
    CallHierarchyHaving _caller = this._getCurrentCaller();
    DeleteTaskRoutine routine = new DeleteTaskRoutine(_routinesFacade, _executionState, _caller, task, container);
    return routine.execute();
  }

  public boolean createAscetTask(final Task task, final ComponentContainer container) {
    Amalthea2ascetRoutinesFacade _routinesFacade = this;
    ReactionExecutionState _executionState = _getExecutionState();
    CallHierarchyHaving _caller = this._getCurrentCaller();
    CreateAscetTaskRoutine routine = new CreateAscetTaskRoutine(_routinesFacade, _executionState, _caller, task, container);
    return routine.execute();
  }

  public boolean createInitTask(final Task task, final ComponentContainer container) {
    Amalthea2ascetRoutinesFacade _routinesFacade = this;
    ReactionExecutionState _executionState = _getExecutionState();
    CallHierarchyHaving _caller = this._getCurrentCaller();
    CreateInitTaskRoutine routine = new CreateInitTaskRoutine(_routinesFacade, _executionState, _caller, task, container);
    return routine.execute();
  }

  public boolean createPeriodicTask(final Task task, final ComponentContainer container) {
    Amalthea2ascetRoutinesFacade _routinesFacade = this;
    ReactionExecutionState _executionState = _getExecutionState();
    CallHierarchyHaving _caller = this._getCurrentCaller();
    CreatePeriodicTaskRoutine routine = new CreatePeriodicTaskRoutine(_routinesFacade, _executionState, _caller, task, container);
    return routine.execute();
  }

  public boolean createSoftwareTask(final Task task, final ComponentContainer container) {
    Amalthea2ascetRoutinesFacade _routinesFacade = this;
    ReactionExecutionState _executionState = _getExecutionState();
    CallHierarchyHaving _caller = this._getCurrentCaller();
    CreateSoftwareTaskRoutine routine = new CreateSoftwareTaskRoutine(_routinesFacade, _executionState, _caller, task, container);
    return routine.execute();
  }

  public boolean createTimeTableTask(final Task task, final ComponentContainer container) {
    Amalthea2ascetRoutinesFacade _routinesFacade = this;
    ReactionExecutionState _executionState = _getExecutionState();
    CallHierarchyHaving _caller = this._getCurrentCaller();
    CreateTimeTableTaskRoutine routine = new CreateTimeTableTaskRoutine(_routinesFacade, _executionState, _caller, task, container);
    return routine.execute();
  }
}
