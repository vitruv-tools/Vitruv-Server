/**
 */
package ecore.tools.vitruv.methodologisttemplate.model.ascet;


/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Periodic Task</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link ecore.tools.vitruv.methodologisttemplate.model.ascet.PeriodicTask#getPeriod <em>Period</em>}</li>
 *   <li>{@link ecore.tools.vitruv.methodologisttemplate.model.ascet.PeriodicTask#getDelay <em>Delay</em>}</li>
 * </ul>
 *
 * @see ecore.tools.vitruv.methodologisttemplate.model.ascet.AscetPackage#getPeriodicTask()
 * @model
 * @generated
 */
public interface PeriodicTask extends AscetTask
{
	/**
	 * Returns the value of the '<em><b>Period</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Period</em>' attribute.
	 * @see #setPeriod(double)
	 * @see ecore.tools.vitruv.methodologisttemplate.model.ascet.AscetPackage#getPeriodicTask_Period()
	 * @model
	 * @generated
	 */
	double getPeriod();

	/**
	 * Sets the value of the '{@link ecore.tools.vitruv.methodologisttemplate.model.ascet.PeriodicTask#getPeriod <em>Period</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Period</em>' attribute.
	 * @see #getPeriod()
	 * @generated
	 */
	void setPeriod(double value);

	/**
	 * Returns the value of the '<em><b>Delay</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Delay</em>' attribute.
	 * @see #setDelay(double)
	 * @see ecore.tools.vitruv.methodologisttemplate.model.ascet.AscetPackage#getPeriodicTask_Delay()
	 * @model
	 * @generated
	 */
	double getDelay();

	/**
	 * Sets the value of the '{@link ecore.tools.vitruv.methodologisttemplate.model.ascet.PeriodicTask#getDelay <em>Delay</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Delay</em>' attribute.
	 * @see #getDelay()
	 * @generated
	 */
	void setDelay(double value);

} // PeriodicTask
