/**
 */
package ecore.tools.vitruv.methodologisttemplate.model.ascet.impl;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.impl.EFactoryImpl;
import org.eclipse.emf.ecore.plugin.EcorePlugin;
import ecore.tools.vitruv.methodologisttemplate.model.ascet.*;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model <b>Factory</b>.
 * <!-- end-user-doc -->
 * @generated
 */
public class AscetFactoryImpl extends EFactoryImpl implements AscetFactory
{
	/**
	 * Creates the default factory implementation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static AscetFactory init()
	{
		try
		{
			AscetFactory theAscetFactory = (AscetFactory)EPackage.Registry.INSTANCE.getEFactory(AscetPackage.eNS_URI);
			if (theAscetFactory != null)
			{
				return theAscetFactory;
			}
		}
		catch (Exception exception)
		{
			EcorePlugin.INSTANCE.log(exception);
		}
		return new AscetFactoryImpl();
	}

	/**
	 * Creates an instance of the factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public AscetFactoryImpl()
	{
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EObject create(EClass eClass)
	{
		switch (eClass.getClassifierID())
		{
			case AscetPackage.ASCET_MODULE: return createAscetModule();
			case AscetPackage.INTERRUPT_TASK: return createInterruptTask();
			case AscetPackage.PERIODIC_TASK: return createPeriodicTask();
			case AscetPackage.SOFTWARE_TASK: return createSoftwareTask();
			case AscetPackage.TIME_TABLE_TASK: return createTimeTableTask();
			case AscetPackage.INIT_TASK: return createInitTask();
			default:
				throw new IllegalArgumentException("The class '" + eClass.getName() + "' is not a valid classifier");
		}
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public AscetModule createAscetModule()
	{
		AscetModuleImpl ascetModule = new AscetModuleImpl();
		return ascetModule;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public InterruptTask createInterruptTask()
	{
		InterruptTaskImpl interruptTask = new InterruptTaskImpl();
		return interruptTask;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public PeriodicTask createPeriodicTask()
	{
		PeriodicTaskImpl periodicTask = new PeriodicTaskImpl();
		return periodicTask;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public SoftwareTask createSoftwareTask()
	{
		SoftwareTaskImpl softwareTask = new SoftwareTaskImpl();
		return softwareTask;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public TimeTableTask createTimeTableTask()
	{
		TimeTableTaskImpl timeTableTask = new TimeTableTaskImpl();
		return timeTableTask;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public InitTask createInitTask()
	{
		InitTaskImpl initTask = new InitTaskImpl();
		return initTask;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public AscetPackage getAscetPackage()
	{
		return (AscetPackage)getEPackage();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @deprecated
	 * @generated
	 */
	@Deprecated
	public static AscetPackage getPackage()
	{
		return AscetPackage.eINSTANCE;
	}

} //AscetFactoryImpl
