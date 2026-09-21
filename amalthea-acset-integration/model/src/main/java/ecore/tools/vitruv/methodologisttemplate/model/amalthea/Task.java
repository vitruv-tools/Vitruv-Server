/**
 */
package ecore.tools.vitruv.methodologisttemplate.model.amalthea;


/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Task</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link ecore.tools.vitruv.methodologisttemplate.model.amalthea.Task#getPreemption <em>Preemption</em>}</li>
 *   <li>{@link ecore.tools.vitruv.methodologisttemplate.model.amalthea.Task#getMultipleTaskActivationLimit <em>Multiple Task Activation Limit</em>}</li>
 * </ul>
 *
 * @see ecore.tools.vitruv.methodologisttemplate.model.amalthea.AmaltheaPackage#getTask()
 * @model
 * @generated
 */
public interface Task extends ecore.tools.vitruv.methodologisttemplate.model.amalthea.Process
{
	/**
	 * Returns the value of the '<em><b>Preemption</b></em>' attribute.
	 * The default value is <code>"cooperative"</code>.
	 * The literals are from the enumeration {@link ecore.tools.vitruv.methodologisttemplate.model.amalthea.PreemptionType}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Preemption</em>' attribute.
	 * @see ecore.tools.vitruv.methodologisttemplate.model.amalthea.PreemptionType
	 * @see #setPreemption(PreemptionType)
	 * @see ecore.tools.vitruv.methodologisttemplate.model.amalthea.AmaltheaPackage#getTask_Preemption()
	 * @model default="cooperative"
	 * @generated
	 */
	PreemptionType getPreemption();

	/**
	 * Sets the value of the '{@link ecore.tools.vitruv.methodologisttemplate.model.amalthea.Task#getPreemption <em>Preemption</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Preemption</em>' attribute.
	 * @see ecore.tools.vitruv.methodologisttemplate.model.amalthea.PreemptionType
	 * @see #getPreemption()
	 * @generated
	 */
	void setPreemption(PreemptionType value);

	/**
	 * Returns the value of the '<em><b>Multiple Task Activation Limit</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Multiple Task Activation Limit</em>' attribute.
	 * @see #setMultipleTaskActivationLimit(int)
	 * @see ecore.tools.vitruv.methodologisttemplate.model.amalthea.AmaltheaPackage#getTask_MultipleTaskActivationLimit()
	 * @model
	 * @generated
	 */
	int getMultipleTaskActivationLimit();

	/**
	 * Sets the value of the '{@link ecore.tools.vitruv.methodologisttemplate.model.amalthea.Task#getMultipleTaskActivationLimit <em>Multiple Task Activation Limit</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Multiple Task Activation Limit</em>' attribute.
	 * @see #getMultipleTaskActivationLimit()
	 * @generated
	 */
	void setMultipleTaskActivationLimit(int value);

} // Task
