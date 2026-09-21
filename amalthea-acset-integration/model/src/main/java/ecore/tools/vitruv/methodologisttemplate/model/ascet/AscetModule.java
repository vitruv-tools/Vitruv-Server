/**
 */
package ecore.tools.vitruv.methodologisttemplate.model.ascet;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Module</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link ecore.tools.vitruv.methodologisttemplate.model.ascet.AscetModule#getTasks <em>Tasks</em>}</li>
 *   <li>{@link ecore.tools.vitruv.methodologisttemplate.model.ascet.AscetModule#getName <em>Name</em>}</li>
 * </ul>
 *
 * @see ecore.tools.vitruv.methodologisttemplate.model.ascet.AscetPackage#getAscetModule()
 * @model
 * @generated
 */
public interface AscetModule extends EObject
{
	/**
	 * Returns the value of the '<em><b>Tasks</b></em>' containment reference list.
	 * The list contents are of type {@link ecore.tools.vitruv.methodologisttemplate.model.ascet.AscetTask}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Tasks</em>' containment reference list.
	 * @see ecore.tools.vitruv.methodologisttemplate.model.ascet.AscetPackage#getAscetModule_Tasks()
	 * @model containment="true"
	 * @generated
	 */
	EList<AscetTask> getTasks();

	/**
	 * Returns the value of the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Name</em>' attribute.
	 * @see #setName(String)
	 * @see ecore.tools.vitruv.methodologisttemplate.model.ascet.AscetPackage#getAscetModule_Name()
	 * @model
	 * @generated
	 */
	String getName();

	/**
	 * Sets the value of the '{@link ecore.tools.vitruv.methodologisttemplate.model.ascet.AscetModule#getName <em>Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Name</em>' attribute.
	 * @see #getName()
	 * @generated
	 */
	void setName(String value);

} // AscetModule
