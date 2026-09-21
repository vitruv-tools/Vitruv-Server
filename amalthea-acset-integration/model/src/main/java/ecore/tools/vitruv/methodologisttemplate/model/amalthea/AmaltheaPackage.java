/**
 */
package ecore.tools.vitruv.methodologisttemplate.model.amalthea;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EEnum;
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
 * @see tools.vitruv.methodologisttemplate.model.amalthea.AmaltheaFactory
 * @model kind="package"
 * @generated
 */
public interface AmaltheaPackage extends EPackage
{
	/**
	 * The package name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String eNAME = "amalthea";

	/**
	 * The package namespace URI.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String eNS_URI = "http://vitruv.tools/reactionsparser/amalthea";

	/**
	 * The package namespace name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String eNS_PREFIX = "amalthea";

	/**
	 * The singleton instance of the package.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	AmaltheaPackage eINSTANCE = ecore.tools.vitruv.methodologisttemplate.model.amalthea.impl.AmaltheaPackageImpl.init();

	/**
	 * The meta object id for the '{@link ecore.tools.vitruv.methodologisttemplate.model.amalthea.impl.ComponentContainerImpl <em>Component Container</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see ecore.tools.vitruv.methodologisttemplate.model.amalthea.impl.ComponentContainerImpl
	 * @see ecore.tools.vitruv.methodologisttemplate.model.amalthea.impl.AmaltheaPackageImpl#getComponentContainer()
	 * @generated
	 */
	int COMPONENT_CONTAINER = 0;

	/**
	 * The feature id for the '<em><b>Tasks</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPONENT_CONTAINER__TASKS = 0;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPONENT_CONTAINER__NAME = 1;

	/**
	 * The number of structural features of the '<em>Component Container</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPONENT_CONTAINER_FEATURE_COUNT = 2;

	/**
	 * The number of operations of the '<em>Component Container</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPONENT_CONTAINER_OPERATION_COUNT = 0;

	/**
	 * The meta object id for the '{@link ecore.tools.vitruv.methodologisttemplate.model.amalthea.impl.ProcessImpl <em>Process</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see ecore.tools.vitruv.methodologisttemplate.model.amalthea.impl.ProcessImpl
	 * @see ecore.tools.vitruv.methodologisttemplate.model.amalthea.impl.AmaltheaPackageImpl#getProcess()
	 * @generated
	 */
	int PROCESS = 1;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PROCESS__NAME = 0;

	/**
	 * The number of structural features of the '<em>Process</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PROCESS_FEATURE_COUNT = 1;

	/**
	 * The number of operations of the '<em>Process</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PROCESS_OPERATION_COUNT = 0;

	/**
	 * The meta object id for the '{@link ecore.tools.vitruv.methodologisttemplate.model.amalthea.impl.ISRImpl <em>ISR</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see ecore.tools.vitruv.methodologisttemplate.model.amalthea.impl.ISRImpl
	 * @see ecore.tools.vitruv.methodologisttemplate.model.amalthea.impl.AmaltheaPackageImpl#getISR()
	 * @generated
	 */
	int ISR = 2;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ISR__NAME = PROCESS__NAME;

	/**
	 * The number of structural features of the '<em>ISR</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ISR_FEATURE_COUNT = PROCESS_FEATURE_COUNT + 0;

	/**
	 * The number of operations of the '<em>ISR</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ISR_OPERATION_COUNT = PROCESS_OPERATION_COUNT + 0;

	/**
	 * The meta object id for the '{@link ecore.tools.vitruv.methodologisttemplate.model.amalthea.impl.TaskImpl <em>Task</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see ecore.tools.vitruv.methodologisttemplate.model.amalthea.impl.TaskImpl
	 * @see ecore.tools.vitruv.methodologisttemplate.model.amalthea.impl.AmaltheaPackageImpl#getTask()
	 * @generated
	 */
	int TASK = 3;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TASK__NAME = PROCESS__NAME;

	/**
	 * The feature id for the '<em><b>Preemption</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TASK__PREEMPTION = PROCESS_FEATURE_COUNT + 0;

	/**
	 * The feature id for the '<em><b>Multiple Task Activation Limit</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TASK__MULTIPLE_TASK_ACTIVATION_LIMIT = PROCESS_FEATURE_COUNT + 1;

	/**
	 * The number of structural features of the '<em>Task</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TASK_FEATURE_COUNT = PROCESS_FEATURE_COUNT + 2;

	/**
	 * The number of operations of the '<em>Task</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TASK_OPERATION_COUNT = PROCESS_OPERATION_COUNT + 0;

	/**
	 * The meta object id for the '{@link ecore.tools.vitruv.methodologisttemplate.model.amalthea.PreemptionType <em>Preemption Type</em>}' enum.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see ecore.tools.vitruv.methodologisttemplate.model.amalthea.PreemptionType
	 * @see ecore.tools.vitruv.methodologisttemplate.model.amalthea.impl.AmaltheaPackageImpl#getPreemptionType()
	 * @generated
	 */
	int PREEMPTION_TYPE = 4;


	/**
	 * Returns the meta object for class '{@link ecore.tools.vitruv.methodologisttemplate.model.amalthea.ComponentContainer <em>Component Container</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Component Container</em>'.
	 * @see ecore.tools.vitruv.methodologisttemplate.model.amalthea.ComponentContainer
	 * @generated
	 */
	EClass getComponentContainer();

	/**
	 * Returns the meta object for the containment reference list '{@link ecore.tools.vitruv.methodologisttemplate.model.amalthea.ComponentContainer#getTasks <em>Tasks</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Tasks</em>'.
	 * @see ecore.tools.vitruv.methodologisttemplate.model.amalthea.ComponentContainer#getTasks()
	 * @see #getComponentContainer()
	 * @generated
	 */
	EReference getComponentContainer_Tasks();

	/**
	 * Returns the meta object for the attribute '{@link ecore.tools.vitruv.methodologisttemplate.model.amalthea.ComponentContainer#getName <em>Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Name</em>'.
	 * @see ecore.tools.vitruv.methodologisttemplate.model.amalthea.ComponentContainer#getName()
	 * @see #getComponentContainer()
	 * @generated
	 */
	EAttribute getComponentContainer_Name();

	/**
	 * Returns the meta object for class '{@link ecore.tools.vitruv.methodologisttemplate.model.amalthea.Process <em>Process</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Process</em>'.
	 * @see ecore.tools.vitruv.methodologisttemplate.model.amalthea.Process
	 * @generated
	 */
	EClass getProcess();

	/**
	 * Returns the meta object for the attribute '{@link ecore.tools.vitruv.methodologisttemplate.model.amalthea.Process#getName <em>Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Name</em>'.
	 * @see ecore.tools.vitruv.methodologisttemplate.model.amalthea.Process#getName()
	 * @see #getProcess()
	 * @generated
	 */
	EAttribute getProcess_Name();

	/**
	 * Returns the meta object for class '{@link ecore.tools.vitruv.methodologisttemplate.model.amalthea.ISR <em>ISR</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>ISR</em>'.
	 * @see ecore.tools.vitruv.methodologisttemplate.model.amalthea.ISR
	 * @generated
	 */
	EClass getISR();

	/**
	 * Returns the meta object for class '{@link ecore.tools.vitruv.methodologisttemplate.model.amalthea.Task <em>Task</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Task</em>'.
	 * @see ecore.tools.vitruv.methodologisttemplate.model.amalthea.Task
	 * @generated
	 */
	EClass getTask();

	/**
	 * Returns the meta object for the attribute '{@link ecore.tools.vitruv.methodologisttemplate.model.amalthea.Task#getPreemption <em>Preemption</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Preemption</em>'.
	 * @see ecore.tools.vitruv.methodologisttemplate.model.amalthea.Task#getPreemption()
	 * @see #getTask()
	 * @generated
	 */
	EAttribute getTask_Preemption();

	/**
	 * Returns the meta object for the attribute '{@link ecore.tools.vitruv.methodologisttemplate.model.amalthea.Task#getMultipleTaskActivationLimit <em>Multiple Task Activation Limit</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Multiple Task Activation Limit</em>'.
	 * @see ecore.tools.vitruv.methodologisttemplate.model.amalthea.Task#getMultipleTaskActivationLimit()
	 * @see #getTask()
	 * @generated
	 */
	EAttribute getTask_MultipleTaskActivationLimit();

	/**
	 * Returns the meta object for enum '{@link ecore.tools.vitruv.methodologisttemplate.model.amalthea.PreemptionType <em>Preemption Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for enum '<em>Preemption Type</em>'.
	 * @see ecore.tools.vitruv.methodologisttemplate.model.amalthea.PreemptionType
	 * @generated
	 */
	EEnum getPreemptionType();

	/**
	 * Returns the factory that creates the instances of the model.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the factory that creates the instances of the model.
	 * @generated
	 */
	AmaltheaFactory getAmaltheaFactory();

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
		 * The meta object literal for the '{@link ecore.tools.vitruv.methodologisttemplate.model.amalthea.impl.ComponentContainerImpl <em>Component Container</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see ecore.tools.vitruv.methodologisttemplate.model.amalthea.impl.ComponentContainerImpl
		 * @see ecore.tools.vitruv.methodologisttemplate.model.amalthea.impl.AmaltheaPackageImpl#getComponentContainer()
		 * @generated
		 */
		EClass COMPONENT_CONTAINER = eINSTANCE.getComponentContainer();

		/**
		 * The meta object literal for the '<em><b>Tasks</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference COMPONENT_CONTAINER__TASKS = eINSTANCE.getComponentContainer_Tasks();

		/**
		 * The meta object literal for the '<em><b>Name</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute COMPONENT_CONTAINER__NAME = eINSTANCE.getComponentContainer_Name();

		/**
		 * The meta object literal for the '{@link ecore.tools.vitruv.methodologisttemplate.model.amalthea.impl.ProcessImpl <em>Process</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see ecore.tools.vitruv.methodologisttemplate.model.amalthea.impl.ProcessImpl
		 * @see ecore.tools.vitruv.methodologisttemplate.model.amalthea.impl.AmaltheaPackageImpl#getProcess()
		 * @generated
		 */
		EClass PROCESS = eINSTANCE.getProcess();

		/**
		 * The meta object literal for the '<em><b>Name</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute PROCESS__NAME = eINSTANCE.getProcess_Name();

		/**
		 * The meta object literal for the '{@link ecore.tools.vitruv.methodologisttemplate.model.amalthea.impl.ISRImpl <em>ISR</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see ecore.tools.vitruv.methodologisttemplate.model.amalthea.impl.ISRImpl
		 * @see ecore.tools.vitruv.methodologisttemplate.model.amalthea.impl.AmaltheaPackageImpl#getISR()
		 * @generated
		 */
		EClass ISR = eINSTANCE.getISR();

		/**
		 * The meta object literal for the '{@link ecore.tools.vitruv.methodologisttemplate.model.amalthea.impl.TaskImpl <em>Task</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see ecore.tools.vitruv.methodologisttemplate.model.amalthea.impl.TaskImpl
		 * @see ecore.tools.vitruv.methodologisttemplate.model.amalthea.impl.AmaltheaPackageImpl#getTask()
		 * @generated
		 */
		EClass TASK = eINSTANCE.getTask();

		/**
		 * The meta object literal for the '<em><b>Preemption</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute TASK__PREEMPTION = eINSTANCE.getTask_Preemption();

		/**
		 * The meta object literal for the '<em><b>Multiple Task Activation Limit</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute TASK__MULTIPLE_TASK_ACTIVATION_LIMIT = eINSTANCE.getTask_MultipleTaskActivationLimit();

		/**
		 * The meta object literal for the '{@link ecore.tools.vitruv.methodologisttemplate.model.amalthea.PreemptionType <em>Preemption Type</em>}' enum.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see ecore.tools.vitruv.methodologisttemplate.model.amalthea.PreemptionType
		 * @see ecore.tools.vitruv.methodologisttemplate.model.amalthea.impl.AmaltheaPackageImpl#getPreemptionType()
		 * @generated
		 */
		EEnum PREEMPTION_TYPE = eINSTANCE.getPreemptionType();

	}

} //AmaltheaPackage
