/**
 */
package ecore.tools.vitruv.methodologisttemplate.model.ascet.impl;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import ecore.tools.vitruv.methodologisttemplate.model.ascet.AscetPackage;
import ecore.tools.vitruv.methodologisttemplate.model.ascet.PeriodicTask;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Periodic Task</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link ecore.tools.vitruv.methodologisttemplate.model.ascet.impl.PeriodicTaskImpl#getPeriod <em>Period</em>}</li>
 *   <li>{@link ecore.tools.vitruv.methodologisttemplate.model.ascet.impl.PeriodicTaskImpl#getDelay <em>Delay</em>}</li>
 * </ul>
 *
 * @generated
 */
public class PeriodicTaskImpl extends AscetTaskImpl implements PeriodicTask
{
	/**
	 * The default value of the '{@link #getPeriod() <em>Period</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getPeriod()
	 * @generated
	 * @ordered
	 */
	protected static final double PERIOD_EDEFAULT = 0.0;

	/**
	 * The cached value of the '{@link #getPeriod() <em>Period</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getPeriod()
	 * @generated
	 * @ordered
	 */
	protected double period = PERIOD_EDEFAULT;

	/**
	 * The default value of the '{@link #getDelay() <em>Delay</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getDelay()
	 * @generated
	 * @ordered
	 */
	protected static final double DELAY_EDEFAULT = 0.0;

	/**
	 * The cached value of the '{@link #getDelay() <em>Delay</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getDelay()
	 * @generated
	 * @ordered
	 */
	protected double delay = DELAY_EDEFAULT;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected PeriodicTaskImpl()
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
		return AscetPackage.Literals.PERIODIC_TASK;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public double getPeriod()
	{
		return period;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setPeriod(double newPeriod)
	{
		double oldPeriod = period;
		period = newPeriod;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, AscetPackage.PERIODIC_TASK__PERIOD, oldPeriod, period));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public double getDelay()
	{
		return delay;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setDelay(double newDelay)
	{
		double oldDelay = delay;
		delay = newDelay;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, AscetPackage.PERIODIC_TASK__DELAY, oldDelay, delay));
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
			case AscetPackage.PERIODIC_TASK__PERIOD:
				return getPeriod();
			case AscetPackage.PERIODIC_TASK__DELAY:
				return getDelay();
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
			case AscetPackage.PERIODIC_TASK__PERIOD:
				setPeriod((Double)newValue);
				return;
			case AscetPackage.PERIODIC_TASK__DELAY:
				setDelay((Double)newValue);
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
			case AscetPackage.PERIODIC_TASK__PERIOD:
				setPeriod(PERIOD_EDEFAULT);
				return;
			case AscetPackage.PERIODIC_TASK__DELAY:
				setDelay(DELAY_EDEFAULT);
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
			case AscetPackage.PERIODIC_TASK__PERIOD:
				return period != PERIOD_EDEFAULT;
			case AscetPackage.PERIODIC_TASK__DELAY:
				return delay != DELAY_EDEFAULT;
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
		result.append(" (period: ");
		result.append(period);
		result.append(", delay: ");
		result.append(delay);
		result.append(')');
		return result.toString();
	}

} //PeriodicTaskImpl
