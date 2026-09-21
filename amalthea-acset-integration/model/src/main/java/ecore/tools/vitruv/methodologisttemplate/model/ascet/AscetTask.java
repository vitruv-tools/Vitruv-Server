/**
 */
package ecore.tools.vitruv.methodologisttemplate.model.ascet;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Task</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link ecore.tools.vitruv.methodologisttemplate.model.ascet.AscetTask#getName <em>Name</em>}</li>
 *   <li>{@link ecore.tools.vitruv.methodologisttemplate.model.ascet.AscetTask#getPriority <em>Priority</em>}</li>
 * </ul>
 *
 * @see ecore.tools.vitruv.methodologisttemplate.model.ascet.AscetPackage#getAscetTask()
 * @model abstract="true"
 * @generated
 */
public interface AscetTask extends EObject
{
	/**
	 * Returns the value of the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Name</em>' attribute.
	 * @see #setName(String)
	 * @see ecore.tools.vitruv.methodologisttemplate.model.ascet.AscetPackage#getAscetTask_Name()
	 * @model
	 * @generated
	 */
	String getName();

	/**
	 * Sets the value of the '{@link ecore.tools.vitruv.methodologisttemplate.model.ascet.AscetTask#getName <em>Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Name</em>' attribute.
	 * @see #getName()
	 * @generated
	 */
	void setName(String value);

	/**
	 * Returns the value of the '<em><b>Priority</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Priority</em>' attribute.
	 * @see #setPriority(int)
	 * @see ecore.tools.vitruv.methodologisttemplate.model.ascet.AscetPackage#getAscetTask_Priority()
	 * @model
	 * @generated
	 */
	int getPriority();

	/**
	 * Sets the value of the '{@link ecore.tools.vitruv.methodologisttemplate.model.ascet.AscetTask#getPriority <em>Priority</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Priority</em>' attribute.
	 * @see #getPriority()
	 * @generated
	 */
	void setPriority(int value);

} // AscetTask
