/**
 */
package ecore.tools.vitruv.methodologisttemplate.model.amalthea.impl;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import ecore.tools.vitruv.methodologisttemplate.model.amalthea.AmaltheaPackage;
import ecore.tools.vitruv.methodologisttemplate.model.amalthea.PreemptionType;
import ecore.tools.vitruv.methodologisttemplate.model.amalthea.Task;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Task</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link ecore.tools.vitruv.methodologisttemplate.model.amalthea.impl.TaskImpl#getPreemption <em>Preemption</em>}</li>
 *   <li>{@link ecore.tools.vitruv.methodologisttemplate.model.amalthea.impl.TaskImpl#getMultipleTaskActivationLimit <em>Multiple Task Activation Limit</em>}</li>
 * </ul>
 *
 * @generated
 */
public class TaskImpl extends ProcessImpl implements Task
{
	/**
	 * The default value of the '{@link #getPreemption() <em>Preemption</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getPreemption()
	 * @generated
	 * @ordered
	 */
	protected static final PreemptionType PREEMPTION_EDEFAULT = PreemptionType.COOPERATIVE;

	/**
	 * The cached value of the '{@link #getPreemption() <em>Preemption</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getPreemption()
	 * @generated
	 * @ordered
	 */
	protected PreemptionType preemption = PREEMPTION_EDEFAULT;

	/**
	 * The default value of the '{@link #getMultipleTaskActivationLimit() <em>Multiple Task Activation Limit</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getMultipleTaskActivationLimit()
	 * @generated
	 * @ordered
	 */
	protected static final int MULTIPLE_TASK_ACTIVATION_LIMIT_EDEFAULT = 0;

	/**
	 * The cached value of the '{@link #getMultipleTaskActivationLimit() <em>Multiple Task Activation Limit</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getMultipleTaskActivationLimit()
	 * @generated
	 * @ordered
	 */
	protected int multipleTaskActivationLimit = MULTIPLE_TASK_ACTIVATION_LIMIT_EDEFAULT;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected TaskImpl()
	{
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass()
	{
		return AmaltheaPackage.Literals.TASK;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public PreemptionType getPreemption()
	{
		return preemption;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setPreemption(PreemptionType newPreemption)
	{
		PreemptionType oldPreemption = preemption;
		preemption = newPreemption == null ? PREEMPTION_EDEFAULT : newPreemption;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, AmaltheaPackage.TASK__PREEMPTION, oldPreemption, preemption));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public int getMultipleTaskActivationLimit()
	{
		return multipleTaskActivationLimit;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setMultipleTaskActivationLimit(int newMultipleTaskActivationLimit)
	{
		int oldMultipleTaskActivationLimit = multipleTaskActivationLimit;
		multipleTaskActivationLimit = newMultipleTaskActivationLimit;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, AmaltheaPackage.TASK__MULTIPLE_TASK_ACTIVATION_LIMIT, oldMultipleTaskActivationLimit, multipleTaskActivationLimit));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType)
	{
		switch (featureID)
		{
			case AmaltheaPackage.TASK__PREEMPTION:
				return getPreemption();
			case AmaltheaPackage.TASK__MULTIPLE_TASK_ACTIVATION_LIMIT:
				return getMultipleTaskActivationLimit();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void eSet(int featureID, Object newValue)
	{
		switch (featureID)
		{
			case AmaltheaPackage.TASK__PREEMPTION:
				setPreemption((PreemptionType)newValue);
				return;
			case AmaltheaPackage.TASK__MULTIPLE_TASK_ACTIVATION_LIMIT:
				setMultipleTaskActivationLimit((Integer)newValue);
				return;
		}
		super.eSet(featureID, newValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void eUnset(int featureID)
	{
		switch (featureID)
		{
			case AmaltheaPackage.TASK__PREEMPTION:
				setPreemption(PREEMPTION_EDEFAULT);
				return;
			case AmaltheaPackage.TASK__MULTIPLE_TASK_ACTIVATION_LIMIT:
				setMultipleTaskActivationLimit(MULTIPLE_TASK_ACTIVATION_LIMIT_EDEFAULT);
				return;
		}
		super.eUnset(featureID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean eIsSet(int featureID)
	{
		switch (featureID)
		{
			case AmaltheaPackage.TASK__PREEMPTION:
				return preemption != PREEMPTION_EDEFAULT;
			case AmaltheaPackage.TASK__MULTIPLE_TASK_ACTIVATION_LIMIT:
				return multipleTaskActivationLimit != MULTIPLE_TASK_ACTIVATION_LIMIT_EDEFAULT;
		}
		return super.eIsSet(featureID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String toString()
	{
		if (eIsProxy()) return super.toString();

		StringBuilder result = new StringBuilder(super.toString());
		result.append(" (preemption: ");
		result.append(preemption);
		result.append(", multipleTaskActivationLimit: ");
		result.append(multipleTaskActivationLimit);
		result.append(')');
		return result.toString();
	}

} //TaskImpl
