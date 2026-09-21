/**
 */
package ecore.tools.vitruv.methodologisttemplate.model.amalthea.impl;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.impl.EFactoryImpl;
import org.eclipse.emf.ecore.plugin.EcorePlugin;
import ecore.tools.vitruv.methodologisttemplate.model.amalthea.AmaltheaFactory;
import ecore.tools.vitruv.methodologisttemplate.model.amalthea.AmaltheaPackage;
import ecore.tools.vitruv.methodologisttemplate.model.amalthea.ComponentContainer;
import ecore.tools.vitruv.methodologisttemplate.model.amalthea.ISR;
import ecore.tools.vitruv.methodologisttemplate.model.amalthea.PreemptionType;
import ecore.tools.vitruv.methodologisttemplate.model.amalthea.Task;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model <b>Factory</b>.
 * <!-- end-user-doc -->
 * @generated
 */
public class AmaltheaFactoryImpl extends EFactoryImpl implements AmaltheaFactory
{
	/**
	 * Creates the default factory implementation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static AmaltheaFactory init()
	{
		try
		{
			AmaltheaFactory theAmaltheaFactory = (AmaltheaFactory)EPackage.Registry.INSTANCE.getEFactory(AmaltheaPackage.eNS_URI);
			if (theAmaltheaFactory != null)
			{
				return theAmaltheaFactory;
			}
		}
		catch (Exception exception)
		{
			EcorePlugin.INSTANCE.log(exception);
		}
		return new AmaltheaFactoryImpl();
	}

	/**
	 * Creates an instance of the factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public AmaltheaFactoryImpl()
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
			case AmaltheaPackage.COMPONENT_CONTAINER: return createComponentContainer();
			case AmaltheaPackage.PROCESS: return createProcess();
			case AmaltheaPackage.ISR: return createISR();
			case AmaltheaPackage.TASK: return createTask();
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
	public Object createFromString(EDataType eDataType, String initialValue)
	{
		switch (eDataType.getClassifierID())
		{
			case AmaltheaPackage.PREEMPTION_TYPE:
				return createPreemptionTypeFromString(eDataType, initialValue);
			default:
				throw new IllegalArgumentException("The datatype '" + eDataType.getName() + "' is not a valid classifier");
		}
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String convertToString(EDataType eDataType, Object instanceValue)
	{
		switch (eDataType.getClassifierID())
		{
			case AmaltheaPackage.PREEMPTION_TYPE:
				return convertPreemptionTypeToString(eDataType, instanceValue);
			default:
				throw new IllegalArgumentException("The datatype '" + eDataType.getName() + "' is not a valid classifier");
		}
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public ComponentContainer createComponentContainer()
	{
		ComponentContainerImpl componentContainer = new ComponentContainerImpl();
		return componentContainer;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public ecore.tools.vitruv.methodologisttemplate.model.amalthea.Process createProcess()
	{
		ProcessImpl process = new ProcessImpl();
		return process;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public ISR createISR()
	{
		ISRImpl isr = new ISRImpl();
		return isr;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Task createTask()
	{
		TaskImpl task = new TaskImpl();
		return task;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public PreemptionType createPreemptionTypeFromString(EDataType eDataType, String initialValue)
	{
		PreemptionType result = PreemptionType.get(initialValue);
		if (result == null) throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
		return result;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String convertPreemptionTypeToString(EDataType eDataType, Object instanceValue)
	{
		return instanceValue == null ? null : instanceValue.toString();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public AmaltheaPackage getAmaltheaPackage()
	{
		return (AmaltheaPackage)getEPackage();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @deprecated
	 * @generated
	 */
	@Deprecated
	public static AmaltheaPackage getPackage()
	{
		return AmaltheaPackage.eINSTANCE;
	}

} //AmaltheaFactoryImpl
