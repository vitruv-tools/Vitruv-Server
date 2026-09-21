/**
 */
package ecore.tools.vitruv.methodologisttemplate.model.amalthea;

import org.eclipse.emf.ecore.EFactory;

/**
 * <!-- begin-user-doc -->
 * The <b>Factory</b> for the model.
 * It provides a create method for each non-abstract class of the model.
 * <!-- end-user-doc -->
 * @see ecore.tools.vitruv.methodologisttemplate.model.amalthea.AmaltheaPackage
 * @generated
 */
public interface AmaltheaFactory extends EFactory
{
	/**
	 * The singleton instance of the factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	AmaltheaFactory eINSTANCE = ecore.tools.vitruv.methodologisttemplate.model.amalthea.impl.AmaltheaFactoryImpl.init();

	/**
	 * Returns a new object of class '<em>Component Container</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Component Container</em>'.
	 * @generated
	 */
	ComponentContainer createComponentContainer();

	/**
	 * Returns a new object of class '<em>Process</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Process</em>'.
	 * @generated
	 */
	Process createProcess();

	/**
	 * Returns a new object of class '<em>ISR</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>ISR</em>'.
	 * @generated
	 */
	ISR createISR();

	/**
	 * Returns a new object of class '<em>Task</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Task</em>'.
	 * @generated
	 */
	Task createTask();

	/**
	 * Returns the package supported by this factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the package supported by this factory.
	 * @generated
	 */
	AmaltheaPackage getAmaltheaPackage();

} //AmaltheaFactory
