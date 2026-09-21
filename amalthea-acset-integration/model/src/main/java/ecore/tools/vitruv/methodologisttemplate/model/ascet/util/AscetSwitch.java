/**
 */
package ecore.tools.vitruv.methodologisttemplate.model.ascet.util;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.util.Switch;
import ecore.tools.vitruv.methodologisttemplate.model.ascet.*;

/**
 * <!-- begin-user-doc -->
 * The <b>Switch</b> for the model's inheritance hierarchy.
 * It supports the call {@link #doSwitch(EObject) doSwitch(object)}
 * to invoke the <code>caseXXX</code> method for each class of the model,
 * starting with the actual class of the object
 * and proceeding up the inheritance hierarchy
 * until a non-null result is returned,
 * which is the result of the switch.
 * <!-- end-user-doc -->
 * @see ecore.tools.vitruv.methodologisttemplate.model.ascet.AscetPackage
 * @generated
 */
public class AscetSwitch<T> extends Switch<T>
{
	/**
	 * The cached model package
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected static AscetPackage modelPackage;

	/**
	 * Creates an instance of the switch.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public AscetSwitch()
	{
		if (modelPackage == null)
		{
			modelPackage = AscetPackage.eINSTANCE;
		}
	}

	/**
	 * Checks whether this is a switch for the given package.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param ePackage the package in question.
	 * @return whether this is a switch for the given package.
	 * @generated
	 */
	@Override
	protected boolean isSwitchFor(EPackage ePackage)
	{
		return ePackage == modelPackage;
	}

	/**
	 * Calls <code>caseXXX</code> for each class of the model until one returns a non null result; it yields that result.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the first non-null result returned by a <code>caseXXX</code> call.
	 * @generated
	 */
	@Override
	protected T doSwitch(int classifierID, EObject theEObject)
	{
		switch (classifierID)
		{
			case AscetPackage.ASCET_MODULE:
			{
				AscetModule ascetModule = (AscetModule)theEObject;
				T result = caseAscetModule(ascetModule);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case AscetPackage.ASCET_TASK:
			{
				AscetTask ascetTask = (AscetTask)theEObject;
				T result = caseAscetTask(ascetTask);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case AscetPackage.INTERRUPT_TASK:
			{
				InterruptTask interruptTask = (InterruptTask)theEObject;
				T result = caseInterruptTask(interruptTask);
				if (result == null) result = caseAscetTask(interruptTask);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case AscetPackage.PERIODIC_TASK:
			{
				PeriodicTask periodicTask = (PeriodicTask)theEObject;
				T result = casePeriodicTask(periodicTask);
				if (result == null) result = caseAscetTask(periodicTask);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case AscetPackage.SOFTWARE_TASK:
			{
				SoftwareTask softwareTask = (SoftwareTask)theEObject;
				T result = caseSoftwareTask(softwareTask);
				if (result == null) result = caseAscetTask(softwareTask);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case AscetPackage.TIME_TABLE_TASK:
			{
				TimeTableTask timeTableTask = (TimeTableTask)theEObject;
				T result = caseTimeTableTask(timeTableTask);
				if (result == null) result = caseAscetTask(timeTableTask);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case AscetPackage.INIT_TASK:
			{
				InitTask initTask = (InitTask)theEObject;
				T result = caseInitTask(initTask);
				if (result == null) result = caseAscetTask(initTask);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			default: return defaultCase(theEObject);
		}
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Module</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Module</em>'.
	 * @see #doSwitch(EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseAscetModule(AscetModule object)
	{
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Task</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Task</em>'.
	 * @see #doSwitch(EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseAscetTask(AscetTask object)
	{
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Interrupt Task</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Interrupt Task</em>'.
	 * @see #doSwitch(EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseInterruptTask(InterruptTask object)
	{
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Periodic Task</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Periodic Task</em>'.
	 * @see #doSwitch(EObject) doSwitch(EObject)
	 * @generated
	 */
	public T casePeriodicTask(PeriodicTask object)
	{
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Software Task</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Software Task</em>'.
	 * @see #doSwitch(EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseSoftwareTask(SoftwareTask object)
	{
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Time Table Task</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Time Table Task</em>'.
	 * @see #doSwitch(EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseTimeTableTask(TimeTableTask object)
	{
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Init Task</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Init Task</em>'.
	 * @see #doSwitch(EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseInitTask(InitTask object)
	{
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>EObject</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch, but this is the last case anyway.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>EObject</em>'.
	 * @see #doSwitch(EObject)
	 * @generated
	 */
	@Override
	public T defaultCase(EObject object)
	{
		return null;
	}

} //AscetSwitch
