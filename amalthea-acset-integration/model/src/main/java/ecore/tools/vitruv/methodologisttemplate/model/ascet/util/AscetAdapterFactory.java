/**
 */
package ecore.tools.vitruv.methodologisttemplate.model.ascet.util;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.common.notify.impl.AdapterFactoryImpl;
import org.eclipse.emf.ecore.EObject;
import ecore.tools.vitruv.methodologisttemplate.model.ascet.*;

/**
 * <!-- begin-user-doc -->
 * The <b>Adapter Factory</b> for the model.
 * It provides an adapter <code>createXXX</code> method for each class of the model.
 * <!-- end-user-doc -->
 * @see ecore.tools.vitruv.methodologisttemplate.model.ascet.AscetPackage
 * @generated
 */
public class AscetAdapterFactory extends AdapterFactoryImpl
{
	/**
	 * The cached model package.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected static AscetPackage modelPackage;

	/**
	 * Creates an instance of the adapter factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public AscetAdapterFactory()
	{
		if (modelPackage == null)
		{
			modelPackage = AscetPackage.eINSTANCE;
		}
	}

	/**
	 * Returns whether this factory is applicable for the type of the object.
	 * <!-- begin-user-doc -->
	 * This implementation returns <code>true</code> if the object is either the model's package or is an instance object of the model.
	 * <!-- end-user-doc -->
	 * @return whether this factory is applicable for the type of the object.
	 * @generated
	 */
	@Override
	public boolean isFactoryForType(Object object)
	{
		if (object == modelPackage)
		{
			return true;
		}
		if (object instanceof EObject)
		{
			return ((EObject)object).eClass().getEPackage() == modelPackage;
		}
		return false;
	}

	/**
	 * The switch that delegates to the <code>createXXX</code> methods.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected AscetSwitch<Adapter> modelSwitch =
		new AscetSwitch<Adapter>()
		{
			@Override
			public Adapter caseAscetModule(AscetModule object)
			{
				return createAscetModuleAdapter();
			}
			@Override
			public Adapter caseAscetTask(AscetTask object)
			{
				return createAscetTaskAdapter();
			}
			@Override
			public Adapter caseInterruptTask(InterruptTask object)
			{
				return createInterruptTaskAdapter();
			}
			@Override
			public Adapter casePeriodicTask(PeriodicTask object)
			{
				return createPeriodicTaskAdapter();
			}
			@Override
			public Adapter caseSoftwareTask(SoftwareTask object)
			{
				return createSoftwareTaskAdapter();
			}
			@Override
			public Adapter caseTimeTableTask(TimeTableTask object)
			{
				return createTimeTableTaskAdapter();
			}
			@Override
			public Adapter caseInitTask(InitTask object)
			{
				return createInitTaskAdapter();
			}
			@Override
			public Adapter defaultCase(EObject object)
			{
				return createEObjectAdapter();
			}
		};

	/**
	 * Creates an adapter for the <code>target</code>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param target the object to adapt.
	 * @return the adapter for the <code>target</code>.
	 * @generated
	 */
	@Override
	public Adapter createAdapter(Notifier target)
	{
		return modelSwitch.doSwitch((EObject)target);
	}


	/**
	 * Creates a new adapter for an object of class '{@link tools.vitruv.methodologisttemplate.model.ascet.AscetModule <em>Module</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see tools.vitruv.methodologisttemplate.model.ascet.AscetModule
	 * @generated
	 */
	public Adapter createAscetModuleAdapter()
	{
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link tools.vitruv.methodologisttemplate.model.ascet.AscetTask <em>Task</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see tools.vitruv.methodologisttemplate.model.ascet.AscetTask
	 * @generated
	 */
	public Adapter createAscetTaskAdapter()
	{
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link tools.vitruv.methodologisttemplate.model.ascet.InterruptTask <em>Interrupt Task</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see tools.vitruv.methodologisttemplate.model.ascet.InterruptTask
	 * @generated
	 */
	public Adapter createInterruptTaskAdapter()
	{
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link tools.vitruv.methodologisttemplate.model.ascet.PeriodicTask <em>Periodic Task</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see tools.vitruv.methodologisttemplate.model.ascet.PeriodicTask
	 * @generated
	 */
	public Adapter createPeriodicTaskAdapter()
	{
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link tools.vitruv.methodologisttemplate.model.ascet.SoftwareTask <em>Software Task</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see tools.vitruv.methodologisttemplate.model.ascet.SoftwareTask
	 * @generated
	 */
	public Adapter createSoftwareTaskAdapter()
	{
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link tools.vitruv.methodologisttemplate.model.ascet.TimeTableTask <em>Time Table Task</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see tools.vitruv.methodologisttemplate.model.ascet.TimeTableTask
	 * @generated
	 */
	public Adapter createTimeTableTaskAdapter()
	{
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link tools.vitruv.methodologisttemplate.model.ascet.InitTask <em>Init Task</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see tools.vitruv.methodologisttemplate.model.ascet.InitTask
	 * @generated
	 */
	public Adapter createInitTaskAdapter()
	{
		return null;
	}

	/**
	 * Creates a new adapter for the default case.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @generated
	 */
	public Adapter createEObjectAdapter()
	{
		return null;
	}

} //AscetAdapterFactory
