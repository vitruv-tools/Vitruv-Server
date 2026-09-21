package edu.neu.ccs.prl.galette.reactions.mir.routines.amalthea2ascet;

import tools.vitruv.dsls.reactions.runtime.routines.AbstractRoutinesFacade;
import tools.vitruv.dsls.reactions.runtime.routines.AbstractRoutinesFacadesProvider;
import tools.vitruv.dsls.reactions.runtime.state.ReactionExecutionState;
import tools.vitruv.dsls.reactions.runtime.structure.ReactionsImportPath;

@SuppressWarnings("all")
public class Amalthea2ascetRoutinesFacadesProvider extends AbstractRoutinesFacadesProvider {
  public Amalthea2ascetRoutinesFacadesProvider(final ReactionExecutionState executionState) {
    super(executionState);
  }

  public AbstractRoutinesFacade createRoutinesFacade(final ReactionsImportPath reactionsImportPath) {
    switch(reactionsImportPath.getPathString()) {
    	case "amalthea2ascet": {
    		return new edu.neu.ccs.prl.galette.reactions.mir.routines.amalthea2ascet.Amalthea2ascetRoutinesFacade(this, reactionsImportPath);
    	}
    	default: {
    		throw new IllegalArgumentException("Unexpected import path: " + reactionsImportPath.getPathString());
    	}
    }
  }
}
