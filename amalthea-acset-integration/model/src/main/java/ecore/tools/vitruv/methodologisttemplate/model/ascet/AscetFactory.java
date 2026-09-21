/**
 */
package ecore.tools.vitruv.methodologisttemplate.model.ascet;

import org.eclipse.emf.ecore.EFactory;

/**
 * <!-- begin-user-doc -->
 * The <b>Factory</b> for the model.
 * It provides a create method for each non-abstract class of the model.
 * <!-- end-user-doc -->
 * @see ecore.tools.vitruv.methodologisttemplate.model.ascet.AscetPackage
 * @generated
 */
public interface AscetFactory extends EFactory
{
	/**
	 * The singleton instance of the factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	AscetFactory eINSTANCE = ecore.tools.vitruv.methodologisttemplate.model.ascet.impl.AscetFactoryImpl.init();

	/**
	 * Returns a new object of class '<em>Module</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Module</em>'.
	 * @generated
	 */
	AscetModule createAscetModule();

	/**
	 * Returns a new object of class '<em>Interrupt Task</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Interrupt Task</em>'.
	 * @generated
	 */
	InterruptTask createInterruptTask();

	/**
	 * Returns a new object of class '<em>Periodic Task</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Periodic Task</em>'.
	 * @generated
	 */
	PeriodicTask createPeriodicTask();

	/**
	 * Returns a new object of class '<em>Software Task</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Software Task</em>'.
	 * @generated
	 */
	SoftwareTask createSoftwareTask();

	/**
	 * Returns a new object of class '<em>Time Table Task</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Time Table Task</em>'.
	 * @generated
	 */
	TimeTableTask createTimeTableTask();

	/**
	 * Returns a new object of class '<em>Init Task</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Init Task</em>'.
	 * @generated
	 */
	InitTask createInitTask();

	/**
	 * Returns the package supported by this factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the package supported by this factory.
	 * @generated
	 */
	AscetPackage getAscetPackage();

} //AscetFactory
