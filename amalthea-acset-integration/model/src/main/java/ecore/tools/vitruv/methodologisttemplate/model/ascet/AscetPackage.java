/**
 */
package ecore.tools.vitruv.methodologisttemplate.model.ascet;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;

/**
 * <!-- begin-user-doc -->
 * The <b>Package</b> for the model.
 * It contains accessors for the meta objects to represent
 * <ul>
 *   <li>each class,</li>
 *   <li>each feature of each class,</li>
 *   <li>each operation of each class,</li>
 *   <li>each enum,</li>
 *   <li>and each data type</li>
 * </ul>
 * <!-- end-user-doc -->
 * @see ecore.tools.vitruv.methodologisttemplate.model.ascet.AscetFactory
 * @model kind="package"
 * @generated
 */
public interface AscetPackage extends EPackage
{
	/**
	 * The package name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String eNAME = "ascet";

	/**
	 * The package namespace URI.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String eNS_URI = "http://vitruv.tools/reactionsparser/ascet";

	/**
	 * The package namespace name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String eNS_PREFIX = "ascet";

	/**
	 * The singleton instance of the package.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	AscetPackage eINSTANCE = ecore.tools.vitruv.methodologisttemplate.model.ascet.impl.AscetPackageImpl.init();

	/**
	 * The meta object id for the '{@link ecore.tools.vitruv.methodologisttemplate.model.ascet.impl.AscetModuleImpl <em>Module</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see ecore.tools.vitruv.methodologisttemplate.model.ascet.impl.AscetModuleImpl
	 * @see ecore.tools.vitruv.methodologisttemplate.model.ascet.impl.AscetPackageImpl#getAscetModule()
	 * @generated
	 */
	int ASCET_MODULE = 0;

	/**
	 * The feature id for the '<em><b>Tasks</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ASCET_MODULE__TASKS = 0;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ASCET_MODULE__NAME = 1;

	/**
	 * The number of structural features of the '<em>Module</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ASCET_MODULE_FEATURE_COUNT = 2;

	/**
	 * The number of operations of the '<em>Module</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ASCET_MODULE_OPERATION_COUNT = 0;

	/**
	 * The meta object id for the '{@link ecore.tools.vitruv.methodologisttemplate.model.ascet.impl.AscetTaskImpl <em>Task</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see ecore.tools.vitruv.methodologisttemplate.model.ascet.impl.AscetTaskImpl
	 * @see ecore.tools.vitruv.methodologisttemplate.model.ascet.impl.AscetPackageImpl#getAscetTask()
	 * @generated
	 */
	int ASCET_TASK = 1;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ASCET_TASK__NAME = 0;

	/**
	 * The feature id for the '<em><b>Priority</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ASCET_TASK__PRIORITY = 1;

	/**
	 * The number of structural features of the '<em>Task</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ASCET_TASK_FEATURE_COUNT = 2;

	/**
	 * The number of operations of the '<em>Task</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ASCET_TASK_OPERATION_COUNT = 0;

	/**
	 * The meta object id for the '{@link ecore.tools.vitruv.methodologisttemplate.model.ascet.impl.InterruptTaskImpl <em>Interrupt Task</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see ecore.tools.vitruv.methodologisttemplate.model.ascet.impl.InterruptTaskImpl
	 * @see ecore.tools.vitruv.methodologisttemplate.model.ascet.impl.AscetPackageImpl#getInterruptTask()
	 * @generated
	 */
	int INTERRUPT_TASK = 2;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INTERRUPT_TASK__NAME = ASCET_TASK__NAME;

	/**
	 * The feature id for the '<em><b>Priority</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INTERRUPT_TASK__PRIORITY = ASCET_TASK__PRIORITY;

	/**
	 * The number of structural features of the '<em>Interrupt Task</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INTERRUPT_TASK_FEATURE_COUNT = ASCET_TASK_FEATURE_COUNT + 0;

	/**
	 * The number of operations of the '<em>Interrupt Task</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INTERRUPT_TASK_OPERATION_COUNT = ASCET_TASK_OPERATION_COUNT + 0;

	/**
	 * The meta object id for the '{@link ecore.tools.vitruv.methodologisttemplate.model.ascet.impl.PeriodicTaskImpl <em>Periodic Task</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see ecore.tools.vitruv.methodologisttemplate.model.ascet.impl.PeriodicTaskImpl
	 * @see ecore.tools.vitruv.methodologisttemplate.model.ascet.impl.AscetPackageImpl#getPeriodicTask()
	 * @generated
	 */
	int PERIODIC_TASK = 3;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PERIODIC_TASK__NAME = ASCET_TASK__NAME;

	/**
	 * The feature id for the '<em><b>Priority</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PERIODIC_TASK__PRIORITY = ASCET_TASK__PRIORITY;

	/**
	 * The feature id for the '<em><b>Period</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PERIODIC_TASK__PERIOD = ASCET_TASK_FEATURE_COUNT + 0;

	/**
	 * The feature id for the '<em><b>Delay</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PERIODIC_TASK__DELAY = ASCET_TASK_FEATURE_COUNT + 1;

	/**
	 * The number of structural features of the '<em>Periodic Task</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PERIODIC_TASK_FEATURE_COUNT = ASCET_TASK_FEATURE_COUNT + 2;

	/**
	 * The number of operations of the '<em>Periodic Task</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PERIODIC_TASK_OPERATION_COUNT = ASCET_TASK_OPERATION_COUNT + 0;

	/**
	 * The meta object id for the '{@link ecore.tools.vitruv.methodologisttemplate.model.ascet.impl.SoftwareTaskImpl <em>Software Task</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see ecore.tools.vitruv.methodologisttemplate.model.ascet.impl.SoftwareTaskImpl
	 * @see ecore.tools.vitruv.methodologisttemplate.model.ascet.impl.AscetPackageImpl#getSoftwareTask()
	 * @generated
	 */
	int SOFTWARE_TASK = 4;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SOFTWARE_TASK__NAME = ASCET_TASK__NAME;

	/**
	 * The feature id for the '<em><b>Priority</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SOFTWARE_TASK__PRIORITY = ASCET_TASK__PRIORITY;

	/**
	 * The number of structural features of the '<em>Software Task</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SOFTWARE_TASK_FEATURE_COUNT = ASCET_TASK_FEATURE_COUNT + 0;

	/**
	 * The number of operations of the '<em>Software Task</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SOFTWARE_TASK_OPERATION_COUNT = ASCET_TASK_OPERATION_COUNT + 0;

	/**
	 * The meta object id for the '{@link ecore.tools.vitruv.methodologisttemplate.model.ascet.impl.TimeTableTaskImpl <em>Time Table Task</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see ecore.tools.vitruv.methodologisttemplate.model.ascet.impl.TimeTableTaskImpl
	 * @see ecore.tools.vitruv.methodologisttemplate.model.ascet.impl.AscetPackageImpl#getTimeTableTask()
	 * @generated
	 */
	int TIME_TABLE_TASK = 5;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TIME_TABLE_TASK__NAME = ASCET_TASK__NAME;

	/**
	 * The feature id for the '<em><b>Priority</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TIME_TABLE_TASK__PRIORITY = ASCET_TASK__PRIORITY;

	/**
	 * The number of structural features of the '<em>Time Table Task</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TIME_TABLE_TASK_FEATURE_COUNT = ASCET_TASK_FEATURE_COUNT + 0;

	/**
	 * The number of operations of the '<em>Time Table Task</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TIME_TABLE_TASK_OPERATION_COUNT = ASCET_TASK_OPERATION_COUNT + 0;

	/**
	 * The meta object id for the '{@link ecore.tools.vitruv.methodologisttemplate.model.ascet.impl.InitTaskImpl <em>Init Task</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see ecore.tools.vitruv.methodologisttemplate.model.ascet.impl.InitTaskImpl
	 * @see ecore.tools.vitruv.methodologisttemplate.model.ascet.impl.AscetPackageImpl#getInitTask()
	 * @generated
	 */
	int INIT_TASK = 6;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INIT_TASK__NAME = ASCET_TASK__NAME;

	/**
	 * The feature id for the '<em><b>Priority</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INIT_TASK__PRIORITY = ASCET_TASK__PRIORITY;

	/**
	 * The number of structural features of the '<em>Init Task</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INIT_TASK_FEATURE_COUNT = ASCET_TASK_FEATURE_COUNT + 0;

	/**
	 * The number of operations of the '<em>Init Task</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INIT_TASK_OPERATION_COUNT = ASCET_TASK_OPERATION_COUNT + 0;


	/**
	 * Returns the meta object for class '{@link ecore.tools.vitruv.methodologisttemplate.model.ascet.AscetModule <em>Module</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Module</em>'.
	 * @see ecore.tools.vitruv.methodologisttemplate.model.ascet.AscetModule
	 * @generated
	 */
	EClass getAscetModule();

	/**
	 * Returns the meta object for the containment reference list '{@link ecore.tools.vitruv.methodologisttemplate.model.ascet.AscetModule#getTasks <em>Tasks</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Tasks</em>'.
	 * @see ecore.tools.vitruv.methodologisttemplate.model.ascet.AscetModule#getTasks()
	 * @see #getAscetModule()
	 * @generated
	 */
	EReference getAscetModule_Tasks();

	/**
	 * Returns the meta object for the attribute '{@link ecore.tools.vitruv.methodologisttemplate.model.ascet.AscetModule#getName <em>Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Name</em>'.
	 * @see ecore.tools.vitruv.methodologisttemplate.model.ascet.AscetModule#getName()
	 * @see #getAscetModule()
	 * @generated
	 */
	EAttribute getAscetModule_Name();

	/**
	 * Returns the meta object for class '{@link ecore.tools.vitruv.methodologisttemplate.model.ascet.AscetTask <em>Task</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Task</em>'.
	 * @see ecore.tools.vitruv.methodologisttemplate.model.ascet.AscetTask
	 * @generated
	 */
	EClass getAscetTask();

	/**
	 * Returns the meta object for the attribute '{@link ecore.tools.vitruv.methodologisttemplate.model.ascet.AscetTask#getName <em>Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Name</em>'.
	 * @see ecore.tools.vitruv.methodologisttemplate.model.ascet.AscetTask#getName()
	 * @see #getAscetTask()
	 * @generated
	 */
	EAttribute getAscetTask_Name();

	/**
	 * Returns the meta object for the attribute '{@link ecore.tools.vitruv.methodologisttemplate.model.ascet.AscetTask#getPriority <em>Priority</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Priority</em>'.
	 * @see ecore.tools.vitruv.methodologisttemplate.model.ascet.AscetTask#getPriority()
	 * @see #getAscetTask()
	 * @generated
	 */
	EAttribute getAscetTask_Priority();

	/**
	 * Returns the meta object for class '{@link ecore.tools.vitruv.methodologisttemplate.model.ascet.InterruptTask <em>Interrupt Task</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Interrupt Task</em>'.
	 * @see ecore.tools.vitruv.methodologisttemplate.model.ascet.InterruptTask
	 * @generated
	 */
	EClass getInterruptTask();

	/**
	 * Returns the meta object for class '{@link ecore.tools.vitruv.methodologisttemplate.model.ascet.PeriodicTask <em>Periodic Task</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Periodic Task</em>'.
	 * @see ecore.tools.vitruv.methodologisttemplate.model.ascet.PeriodicTask
	 * @generated
	 */
	EClass getPeriodicTask();

	/**
	 * Returns the meta object for the attribute '{@link ecore.tools.vitruv.methodologisttemplate.model.ascet.PeriodicTask#getPeriod <em>Period</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Period</em>'.
	 * @see ecore.tools.vitruv.methodologisttemplate.model.ascet.PeriodicTask#getPeriod()
	 * @see #getPeriodicTask()
	 * @generated
	 */
	EAttribute getPeriodicTask_Period();

	/**
	 * Returns the meta object for the attribute '{@link ecore.tools.vitruv.methodologisttemplate.model.ascet.PeriodicTask#getDelay <em>Delay</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Delay</em>'.
	 * @see ecore.tools.vitruv.methodologisttemplate.model.ascet.PeriodicTask#getDelay()
	 * @see #getPeriodicTask()
	 * @generated
	 */
	EAttribute getPeriodicTask_Delay();

	/**
	 * Returns the meta object for class '{@link ecore.tools.vitruv.methodologisttemplate.model.ascet.SoftwareTask <em>Software Task</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Software Task</em>'.
	 * @see ecore.tools.vitruv.methodologisttemplate.model.ascet.SoftwareTask
	 * @generated
	 */
	EClass getSoftwareTask();

	/**
	 * Returns the meta object for class '{@link ecore.tools.vitruv.methodologisttemplate.model.ascet.TimeTableTask <em>Time Table Task</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Time Table Task</em>'.
	 * @see ecore.tools.vitruv.methodologisttemplate.model.ascet.TimeTableTask
	 * @generated
	 */
	EClass getTimeTableTask();

	/**
	 * Returns the meta object for class '{@link ecore.tools.vitruv.methodologisttemplate.model.ascet.InitTask <em>Init Task</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Init Task</em>'.
	 * @see ecore.tools.vitruv.methodologisttemplate.model.ascet.InitTask
	 * @generated
	 */
	EClass getInitTask();

	/**
	 * Returns the factory that creates the instances of the model.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the factory that creates the instances of the model.
	 * @generated
	 */
	AscetFactory getAscetFactory();

	/**
	 * <!-- begin-user-doc -->
	 * Defines literals for the meta objects that represent
	 * <ul>
	 *   <li>each class,</li>
	 *   <li>each feature of each class,</li>
	 *   <li>each operation of each class,</li>
	 *   <li>each enum,</li>
	 *   <li>and each data type</li>
	 * </ul>
	 * <!-- end-user-doc -->
	 * @generated
	 */
	interface Literals
	{
		/**
		 * The meta object literal for the '{@link ecore.tools.vitruv.methodologisttemplate.model.ascet.impl.AscetModuleImpl <em>Module</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see ecore.tools.vitruv.methodologisttemplate.model.ascet.impl.AscetModuleImpl
		 * @see ecore.tools.vitruv.methodologisttemplate.model.ascet.impl.AscetPackageImpl#getAscetModule()
		 * @generated
		 */
		EClass ASCET_MODULE = eINSTANCE.getAscetModule();

		/**
		 * The meta object literal for the '<em><b>Tasks</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference ASCET_MODULE__TASKS = eINSTANCE.getAscetModule_Tasks();

		/**
		 * The meta object literal for the '<em><b>Name</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute ASCET_MODULE__NAME = eINSTANCE.getAscetModule_Name();

		/**
		 * The meta object literal for the '{@link ecore.tools.vitruv.methodologisttemplate.model.ascet.impl.AscetTaskImpl <em>Task</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see ecore.tools.vitruv.methodologisttemplate.model.ascet.impl.AscetTaskImpl
		 * @see ecore.tools.vitruv.methodologisttemplate.model.ascet.impl.AscetPackageImpl#getAscetTask()
		 * @generated
		 */
		EClass ASCET_TASK = eINSTANCE.getAscetTask();

		/**
		 * The meta object literal for the '<em><b>Name</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute ASCET_TASK__NAME = eINSTANCE.getAscetTask_Name();

		/**
		 * The meta object literal for the '<em><b>Priority</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute ASCET_TASK__PRIORITY = eINSTANCE.getAscetTask_Priority();

		/**
		 * The meta object literal for the '{@link ecore.tools.vitruv.methodologisttemplate.model.ascet.impl.InterruptTaskImpl <em>Interrupt Task</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see ecore.tools.vitruv.methodologisttemplate.model.ascet.impl.InterruptTaskImpl
		 * @see ecore.tools.vitruv.methodologisttemplate.model.ascet.impl.AscetPackageImpl#getInterruptTask()
		 * @generated
		 */
		EClass INTERRUPT_TASK = eINSTANCE.getInterruptTask();

		/**
		 * The meta object literal for the '{@link ecore.tools.vitruv.methodologisttemplate.model.ascet.impl.PeriodicTaskImpl <em>Periodic Task</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see ecore.tools.vitruv.methodologisttemplate.model.ascet.impl.PeriodicTaskImpl
		 * @see ecore.tools.vitruv.methodologisttemplate.model.ascet.impl.AscetPackageImpl#getPeriodicTask()
		 * @generated
		 */
		EClass PERIODIC_TASK = eINSTANCE.getPeriodicTask();

		/**
		 * The meta object literal for the '<em><b>Period</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute PERIODIC_TASK__PERIOD = eINSTANCE.getPeriodicTask_Period();

		/**
		 * The meta object literal for the '<em><b>Delay</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute PERIODIC_TASK__DELAY = eINSTANCE.getPeriodicTask_Delay();

		/**
		 * The meta object literal for the '{@link ecore.tools.vitruv.methodologisttemplate.model.ascet.impl.SoftwareTaskImpl <em>Software Task</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see ecore.tools.vitruv.methodologisttemplate.model.ascet.impl.SoftwareTaskImpl
		 * @see ecore.tools.vitruv.methodologisttemplate.model.ascet.impl.AscetPackageImpl#getSoftwareTask()
		 * @generated
		 */
		EClass SOFTWARE_TASK = eINSTANCE.getSoftwareTask();

		/**
		 * The meta object literal for the '{@link ecore.tools.vitruv.methodologisttemplate.model.ascet.impl.TimeTableTaskImpl <em>Time Table Task</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see ecore.tools.vitruv.methodologisttemplate.model.ascet.impl.TimeTableTaskImpl
		 * @see ecore.tools.vitruv.methodologisttemplate.model.ascet.impl.AscetPackageImpl#getTimeTableTask()
		 * @generated
		 */
		EClass TIME_TABLE_TASK = eINSTANCE.getTimeTableTask();

		/**
		 * The meta object literal for the '{@link ecore.tools.vitruv.methodologisttemplate.model.ascet.impl.InitTaskImpl <em>Init Task</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see ecore.tools.vitruv.methodologisttemplate.model.ascet.impl.InitTaskImpl
		 * @see ecore.tools.vitruv.methodologisttemplate.model.ascet.impl.AscetPackageImpl#getInitTask()
		 * @generated
		 */
		EClass INIT_TASK = eINSTANCE.getInitTask();

	}

} //AscetPackage
