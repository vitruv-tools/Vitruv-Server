package edu.neu.ccs.prl.galette.reactions.mir.routines.amalthea2ascet;

import java.io.IOException;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.xbase.lib.Extension;
import tools.vitruv.dsls.reactions.runtime.routines.AbstractRoutine;
import tools.vitruv.dsls.reactions.runtime.state.ReactionExecutionState;
import tools.vitruv.dsls.reactions.runtime.structure.CallHierarchyHaving;
import ecore.tools.vitruv.methodologisttemplate.model.amalthea.ComponentContainer;
import ecore.tools.vitruv.methodologisttemplate.model.ascet.AscetModule;

@SuppressWarnings("all")
public class CreateAndRegisterRootComponentContainerRoutine extends AbstractRoutine {
  private InputValues inputValues;

  private Match.RetrievedValues retrievedValues;

  private Create.CreatedValues createdValues;

  public class InputValues {
    public final ComponentContainer componentContainer;

    public InputValues(final ComponentContainer componentContainer) {
      this.componentContainer = componentContainer;
    }
  }

  private static class Match extends AbstractRoutine.Match {
    public class RetrievedValues {
      public RetrievedValues() {
        
      }
    }

    public Match(final ReactionExecutionState reactionExecutionState) {
      super(reactionExecutionState);
    }

    public EObject getCorrepondenceSource1(final ComponentContainer componentContainer) {
      return componentContainer;
    }

    public RetrievedValues match(final ComponentContainer componentContainer) throws IOException {
      if (hasCorrespondingElements(
      	getCorrepondenceSource1(componentContainer), // correspondence source supplier
          ecore.tools.vitruv.methodologisttemplate.model.ascet.AscetModule.class,
      	null, // correspondence precondition checker
      	null
      )) {
      	return null;
      }
      return new edu.neu.ccs.prl.galette.reactions.mir.routines.amalthea2ascet.CreateAndRegisterRootComponentContainerRoutine.Match.RetrievedValues();
    }
  }

  private static class Create extends AbstractRoutine.Create {
    public class CreatedValues {
      public final AscetModule mRoot;

      public CreatedValues(final AscetModule mRoot) {
        this.mRoot = mRoot;
      }
    }

    public Create(final ReactionExecutionState reactionExecutionState) {
      super(reactionExecutionState);
    }

    public CreatedValues createElements() {
      ecore.tools.vitruv.methodologisttemplate.model.ascet.AscetModule mRoot = createObject(() -> {
      	return ecore.tools.vitruv.methodologisttemplate.model.ascet.impl.AscetFactoryImpl.eINSTANCE.createAscetModule();
      });
      return new CreatedValues(mRoot);
    }
  }

  private static class Update extends AbstractRoutine.Update {
    public Update(final ReactionExecutionState reactionExecutionState) {
      super(reactionExecutionState);
    }

    public void updateModels(final ComponentContainer componentContainer, final AscetModule mRoot, @Extension final Amalthea2ascetRoutinesFacade _routinesFacade) {
      this.persistProjectRelative(componentContainer, mRoot, "example.model2");
      this.addCorrespondenceBetween(componentContainer, mRoot);
      this.addCorrespondenceBetween(mRoot, componentContainer);
    }
  }

  public CreateAndRegisterRootComponentContainerRoutine(final Amalthea2ascetRoutinesFacade routinesFacade, final ReactionExecutionState reactionExecutionState, final CallHierarchyHaving calledBy, final ComponentContainer componentContainer) {
    super(routinesFacade, reactionExecutionState, calledBy);
    this.inputValues = new InputValues(componentContainer);
  }

  protected boolean executeRoutine() throws IOException {
    if (getLogger().isTraceEnabled()) {
    	getLogger().trace("Called routine CreateAndRegisterRootComponentContainerRoutine with input:");
    	getLogger().trace("   inputValues.componentContainer: " + inputValues.componentContainer);
    }
    retrievedValues = new edu.neu.ccs.prl.galette.reactions.mir.routines.amalthea2ascet.CreateAndRegisterRootComponentContainerRoutine.Match(getExecutionState()).match(inputValues.componentContainer);
    if (retrievedValues == null) {
    	return false;
    }
    createdValues = new edu.neu.ccs.prl.galette.reactions.mir.routines.amalthea2ascet.CreateAndRegisterRootComponentContainerRoutine.Create(getExecutionState()).createElements();
    new edu.neu.ccs.prl.galette.reactions.mir.routines.amalthea2ascet.CreateAndRegisterRootComponentContainerRoutine.Update(getExecutionState()).updateModels(inputValues.componentContainer, createdValues.mRoot, getRoutinesFacade());
    return true;
  }
}
