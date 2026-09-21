/**
 */
package ecore.tools.vitruv.methodologisttemplate.model.amalthea;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Component Container</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link ecore.tools.vitruv.methodologisttemplate.model.amalthea.ComponentContainer#getTasks <em>Tasks</em>}</li>
 *   <li>{@link ecore.tools.vitruv.methodologisttemplate.model.amalthea.ComponentContainer#getName <em>Name</em>}</li>
 * </ul>
 *
 * @see ecore.tools.vitruv.methodologisttemplate.model.amalthea.AmaltheaPackage#getComponentContainer()
 * @model
 * @generated
 */
public interface ComponentContainer extends EObject
{
	/**
	 * Returns the value of the '<em><b>Tasks</b></em>' containment reference list.
	 * The list contents are of type {@link ecore.tools.vitruv.methodologisttemplate.model.amalthea.Task}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Tasks</em>' containment reference list.
	 * @see ecore.tools.vitruv.methodologisttemplate.model.amalthea.AmaltheaPackage#getComponentContainer_Tasks()
	 * @model containment="true"
	 * @generated
	 */
	EList<Task> getTasks();

	/**
	 * Returns the value of the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Name</em>' attribute.
	 * @see #setName(String)
	 * @see ecore.tools.vitruv.methodologisttemplate.model.amalthea.AmaltheaPackage#getComponentContainer_Name()
	 * @model
	 * @generated
	 */
	String getName();

	/**
	 * Sets the value of the '{@link ecore.tools.vitruv.methodologisttemplate.model.amalthea.ComponentContainer#getName <em>Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Name</em>' attribute.
	 * @see #getName()
	 * @generated
	 */
	void setName(String value);

} // ComponentContainer
