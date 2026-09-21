/**
 */
package ecore.tools.vitruv.methodologisttemplate.model.ascet.impl;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.impl.EPackageImpl;
import ecore.tools.vitruv.methodologisttemplate.model.ascet.AscetFactory;
import ecore.tools.vitruv.methodologisttemplate.model.ascet.AscetModule;
import ecore.tools.vitruv.methodologisttemplate.model.ascet.AscetPackage;
import ecore.tools.vitruv.methodologisttemplate.model.ascet.AscetTask;
import ecore.tools.vitruv.methodologisttemplate.model.ascet.InitTask;
import ecore.tools.vitruv.methodologisttemplate.model.ascet.InterruptTask;
import ecore.tools.vitruv.methodologisttemplate.model.ascet.PeriodicTask;
import ecore.tools.vitruv.methodologisttemplate.model.ascet.SoftwareTask;
import ecore.tools.vitruv.methodologisttemplate.model.ascet.TimeTableTask;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model <b>Package</b>.
 * <!-- end-user-doc -->
 * @generated
 */
public class AscetPackageImpl extends EPackageImpl implements AscetPackage
{
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass ascetModuleEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass ascetTaskEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass interruptTaskEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass periodicTaskEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass softwareTaskEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass timeTableTaskEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass initTaskEClass = null;

	/**
	 * Creates an instance of the model <b>Package</b>, registered with
	 * {@link Registry EPackage.Registry} by the package
	 * package URI value.
	 * <p>Note: the correct way to create the package is via the static
	 * factory method {@link #init init()}, which also performs
	 * initialization of the package, or returns the registered package,
	 * if one already exists.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see Registry
	 * @see AscetPackage#eNS_URI
	 * @see #init()
	 * @generated
	 */
	private AscetPackageImpl()
	{
		super(eNS_URI, AscetFactory.eINSTANCE);
	}
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private static boolean isInited = false;

	/**
	 * Creates, registers, and initializes the <b>Package</b> for this model, and for any others upon which it depends.
	 *
	 * <p>This method is used to initialize {@link AscetPackage#eINSTANCE} when that field is accessed.
	 * Clients should not invoke it directly. Instead, they should simply access that field to obtain the package.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #eNS_URI
	 * @see #createPackageContents()
	 * @see #initializePackageContents()
	 * @generated
	 */
	public static AscetPackage init()
	{
		if (isInited) return (AscetPackage) Registry.INSTANCE.getEPackage(AscetPackage.eNS_URI);

		// Obtain or create and register package
		Object registeredAscetPackage = Registry.INSTANCE.get(eNS_URI);
		AscetPackageImpl theAscetPackage = registeredAscetPackage instanceof AscetPackageImpl ? (AscetPackageImpl)registeredAscetPackage : new AscetPackageImpl();

		isInited = true;

		// Create package meta-data objects
		theAscetPackage.createPackageContents();

		// Initialize created meta-data
		theAscetPackage.initializePackageContents();

		// Mark meta-data to indicate it can't be changed
		theAscetPackage.freeze();

		// Update the registry and return the package
		Registry.INSTANCE.put(AscetPackage.eNS_URI, theAscetPackage);
		return theAscetPackage;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getAscetModule()
	{
		return ascetModuleEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getAscetModule_Tasks()
	{
		return (EReference)ascetModuleEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getAscetModule_Name()
	{
		return (EAttribute)ascetModuleEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getAscetTask()
	{
		return ascetTaskEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getAscetTask_Name()
	{
		return (EAttribute)ascetTaskEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getAscetTask_Priority()
	{
		return (EAttribute)ascetTaskEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getInterruptTask()
	{
		return interruptTaskEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getPeriodicTask()
	{
		return periodicTaskEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getPeriodicTask_Period()
	{
		return (EAttribute)periodicTaskEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getPeriodicTask_Delay()
	{
		return (EAttribute)periodicTaskEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getSoftwareTask()
	{
		return softwareTaskEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getTimeTableTask()
	{
		return timeTableTaskEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getInitTask()
	{
		return initTaskEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public AscetFactory getAscetFactory()
	{
		return (AscetFactory)getEFactoryInstance();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private boolean isCreated = false;

	/**
	 * Creates the meta-model objects for the package.  This method is
	 * guarded to have no affect on any invocation but its first.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void createPackageContents()
	{
		if (isCreated) return;
		isCreated = true;

		// Create classes and their features
		ascetModuleEClass = createEClass(ASCET_MODULE);
		createEReference(ascetModuleEClass, ASCET_MODULE__TASKS);
		createEAttribute(ascetModuleEClass, ASCET_MODULE__NAME);

		ascetTaskEClass = createEClass(ASCET_TASK);
		createEAttribute(ascetTaskEClass, ASCET_TASK__NAME);
		createEAttribute(ascetTaskEClass, ASCET_TASK__PRIORITY);

		interruptTaskEClass = createEClass(INTERRUPT_TASK);

		periodicTaskEClass = createEClass(PERIODIC_TASK);
		createEAttribute(periodicTaskEClass, PERIODIC_TASK__PERIOD);
		createEAttribute(periodicTaskEClass, PERIODIC_TASK__DELAY);

		softwareTaskEClass = createEClass(SOFTWARE_TASK);

		timeTableTaskEClass = createEClass(TIME_TABLE_TASK);

		initTaskEClass = createEClass(INIT_TASK);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private boolean isInitialized = false;

	/**
	 * Complete the initialization of the package and its meta-model.  This
	 * method is guarded to have no affect on any invocation but its first.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void initializePackageContents()
	{
		if (isInitialized) return;
		isInitialized = true;

		// Initialize package
		setName(eNAME);
		setNsPrefix(eNS_PREFIX);
		setNsURI(eNS_URI);

		// Create type parameters

		// Set bounds for type parameters

		// Add supertypes to classes
		interruptTaskEClass.getESuperTypes().add(this.getAscetTask());
		periodicTaskEClass.getESuperTypes().add(this.getAscetTask());
		softwareTaskEClass.getESuperTypes().add(this.getAscetTask());
		timeTableTaskEClass.getESuperTypes().add(this.getAscetTask());
		initTaskEClass.getESuperTypes().add(this.getAscetTask());

		// Initialize classes, features, and operations; add parameters
		initEClass(ascetModuleEClass, AscetModule.class, "AscetModule", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEReference(getAscetModule_Tasks(), this.getAscetTask(), null, "tasks", null, 0, -1, AscetModule.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getAscetModule_Name(), ecorePackage.getEString(), "name", null, 0, 1, AscetModule.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(ascetTaskEClass, AscetTask.class, "AscetTask", IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getAscetTask_Name(), ecorePackage.getEString(), "name", null, 0, 1, AscetTask.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getAscetTask_Priority(), ecorePackage.getEInt(), "priority", null, 0, 1, AscetTask.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(interruptTaskEClass, InterruptTask.class, "InterruptTask", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);

		initEClass(periodicTaskEClass, PeriodicTask.class, "PeriodicTask", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getPeriodicTask_Period(), ecorePackage.getEDouble(), "period", null, 0, 1, PeriodicTask.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getPeriodicTask_Delay(), ecorePackage.getEDouble(), "delay", null, 0, 1, PeriodicTask.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(softwareTaskEClass, SoftwareTask.class, "SoftwareTask", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);

		initEClass(timeTableTaskEClass, TimeTableTask.class, "TimeTableTask", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);

		initEClass(initTaskEClass, InitTask.class, "InitTask", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);

		// Create resource
		createResource(eNS_URI);
	}

} //AscetPackageImpl
