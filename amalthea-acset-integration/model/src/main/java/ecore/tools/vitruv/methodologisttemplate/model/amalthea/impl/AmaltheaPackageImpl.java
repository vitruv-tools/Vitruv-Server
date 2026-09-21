/**
 */
package ecore.tools.vitruv.methodologisttemplate.model.amalthea.impl;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.impl.EPackageImpl;
import ecore.tools.vitruv.methodologisttemplate.model.amalthea.AmaltheaFactory;
import ecore.tools.vitruv.methodologisttemplate.model.amalthea.AmaltheaPackage;
import ecore.tools.vitruv.methodologisttemplate.model.amalthea.ComponentContainer;
import ecore.tools.vitruv.methodologisttemplate.model.amalthea.PreemptionType;
import ecore.tools.vitruv.methodologisttemplate.model.amalthea.Task;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model <b>Package</b>.
 * <!-- end-user-doc -->
 * @generated
 */
public class AmaltheaPackageImpl extends EPackageImpl implements AmaltheaPackage
{
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass componentContainerEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass processEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass isrEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass taskEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EEnum preemptionTypeEEnum = null;

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
	 * @see AmaltheaPackage#eNS_URI
	 * @see #init()
	 * @generated
	 */
	private AmaltheaPackageImpl()
	{
		super(eNS_URI, AmaltheaFactory.eINSTANCE);
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
	 * <p>This method is used to initialize {@link AmaltheaPackage#eINSTANCE} when that field is accessed.
	 * Clients should not invoke it directly. Instead, they should simply access that field to obtain the package.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #eNS_URI
	 * @see #createPackageContents()
	 * @see #initializePackageContents()
	 * @generated
	 */
	public static AmaltheaPackage init()
	{
		if (isInited) return (AmaltheaPackage) Registry.INSTANCE.getEPackage(AmaltheaPackage.eNS_URI);

		// Obtain or create and register package
		Object registeredAmaltheaPackage = Registry.INSTANCE.get(eNS_URI);
		AmaltheaPackageImpl theAmaltheaPackage = registeredAmaltheaPackage instanceof AmaltheaPackageImpl ? (AmaltheaPackageImpl)registeredAmaltheaPackage : new AmaltheaPackageImpl();

		isInited = true;

		// Create package meta-data objects
		theAmaltheaPackage.createPackageContents();

		// Initialize created meta-data
		theAmaltheaPackage.initializePackageContents();

		// Mark meta-data to indicate it can't be changed
		theAmaltheaPackage.freeze();

		// Update the registry and return the package
		Registry.INSTANCE.put(AmaltheaPackage.eNS_URI, theAmaltheaPackage);
		return theAmaltheaPackage;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getComponentContainer()
	{
		return componentContainerEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getComponentContainer_Tasks()
	{
		return (EReference)componentContainerEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getComponentContainer_Name()
	{
		return (EAttribute)componentContainerEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getProcess()
	{
		return processEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getProcess_Name()
	{
		return (EAttribute)processEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getISR()
	{
		return isrEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getTask()
	{
		return taskEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getTask_Preemption()
	{
		return (EAttribute)taskEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getTask_MultipleTaskActivationLimit()
	{
		return (EAttribute)taskEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EEnum getPreemptionType()
	{
		return preemptionTypeEEnum;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public AmaltheaFactory getAmaltheaFactory()
	{
		return (AmaltheaFactory)getEFactoryInstance();
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
		componentContainerEClass = createEClass(COMPONENT_CONTAINER);
		createEReference(componentContainerEClass, COMPONENT_CONTAINER__TASKS);
		createEAttribute(componentContainerEClass, COMPONENT_CONTAINER__NAME);

		processEClass = createEClass(PROCESS);
		createEAttribute(processEClass, PROCESS__NAME);

		isrEClass = createEClass(ISR);

		taskEClass = createEClass(TASK);
		createEAttribute(taskEClass, TASK__PREEMPTION);
		createEAttribute(taskEClass, TASK__MULTIPLE_TASK_ACTIVATION_LIMIT);

		// Create enums
		preemptionTypeEEnum = createEEnum(PREEMPTION_TYPE);
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
		isrEClass.getESuperTypes().add(this.getProcess());
		taskEClass.getESuperTypes().add(this.getProcess());

		// Initialize classes, features, and operations; add parameters
		initEClass(componentContainerEClass, ComponentContainer.class, "ComponentContainer", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEReference(getComponentContainer_Tasks(), this.getTask(), null, "tasks", null, 0, -1, ComponentContainer.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getComponentContainer_Name(), ecorePackage.getEString(), "name", null, 0, 1, ComponentContainer.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(processEClass, ecore.tools.vitruv.methodologisttemplate.model.amalthea.Process.class, "Process", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getProcess_Name(), ecorePackage.getEString(), "name", null, 0, 1, ecore.tools.vitruv.methodologisttemplate.model.amalthea.Process.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(isrEClass, ecore.tools.vitruv.methodologisttemplate.model.amalthea.ISR.class, "ISR", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);

		initEClass(taskEClass, Task.class, "Task", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getTask_Preemption(), this.getPreemptionType(), "preemption", "cooperative", 0, 1, Task.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getTask_MultipleTaskActivationLimit(), ecorePackage.getEInt(), "multipleTaskActivationLimit", null, 0, 1, Task.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		// Initialize enums and add enum literals
		initEEnum(preemptionTypeEEnum, PreemptionType.class, "PreemptionType");
		addEEnumLiteral(preemptionTypeEEnum, PreemptionType.COOPERATIVE);
		addEEnumLiteral(preemptionTypeEEnum, PreemptionType.PREEMPTIVE);

		// Create resource
		createResource(eNS_URI);
	}

} //AmaltheaPackageImpl
