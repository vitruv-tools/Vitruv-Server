package edu.neu.ccs.prl.galette.reactions.mir.reactions.amalthea2ascet;

import java.util.Set;
import tools.vitruv.change.composite.MetamodelDescriptor;
import tools.vitruv.change.propagation.ChangePropagationSpecification;
import tools.vitruv.dsls.reactions.runtime.reactions.AbstractReactionsChangePropagationSpecification;
import tools.vitruv.dsls.reactions.runtime.routines.RoutinesFacadesProvider;
import tools.vitruv.dsls.reactions.runtime.state.ReactionExecutionState;
import tools.vitruv.dsls.reactions.runtime.structure.ReactionsImportPath;

@SuppressWarnings("all")
public class Amalthea2ascetChangePropagationSpecification extends AbstractReactionsChangePropagationSpecification implements ChangePropagationSpecification {
  public Amalthea2ascetChangePropagationSpecification() {
    super(MetamodelDescriptor.with(Set.of("http://vitruv.tools/reactionsparser/amalthea")), 
    	MetamodelDescriptor.with(Set.of("http://vitruv.tools/reactionsparser/ascet")));
  }

  protected RoutinesFacadesProvider createRoutinesFacadesProvider(final ReactionExecutionState executionState) {
    return new  edu.neu.ccs.prl.galette.reactions.mir.routines.amalthea2ascet.Amalthea2ascetRoutinesFacadesProvider(executionState);
  }

  protected void setup() {
    org.eclipse.emf.ecore.EPackage.Registry.INSTANCE.putIfAbsent(ecore.tools.vitruv.methodologisttemplate.model.amalthea.impl.AmaltheaPackageImpl.eNS_URI, ecore.tools.vitruv.methodologisttemplate.model.amalthea.impl.AmaltheaPackageImpl.eINSTANCE);
    org.eclipse.emf.ecore.EPackage.Registry.INSTANCE.putIfAbsent(ecore.tools.vitruv.methodologisttemplate.model.ascet.impl.AscetPackageImpl.eNS_URI, ecore.tools.vitruv.methodologisttemplate.model.ascet.impl.AscetPackageImpl.eINSTANCE);
    addReaction(new edu.neu.ccs.prl.galette.reactions.mir.reactions.amalthea2ascet.ComponentContainerInsertedAsRootReaction((executionState) -> createRoutinesFacadesProvider(executionState).getRoutinesFacade(ReactionsImportPath.fromPathString("amalthea2ascet"))));
    addReaction(new edu.neu.ccs.prl.galette.reactions.mir.reactions.amalthea2ascet.TaskDeletedReaction((executionState) -> createRoutinesFacadesProvider(executionState).getRoutinesFacade(ReactionsImportPath.fromPathString("amalthea2ascet"))));
    addReaction(new edu.neu.ccs.prl.galette.reactions.mir.reactions.amalthea2ascet.TaskCreatedReaction((executionState) -> createRoutinesFacadesProvider(executionState).getRoutinesFacade(ReactionsImportPath.fromPathString("amalthea2ascet"))));
  }
}
