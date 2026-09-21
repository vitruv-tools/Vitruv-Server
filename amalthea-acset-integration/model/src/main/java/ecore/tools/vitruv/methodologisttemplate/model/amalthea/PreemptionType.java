/**
 */
package ecore.tools.vitruv.methodologisttemplate.model.amalthea;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.eclipse.emf.common.util.Enumerator;

/**
 * <!-- begin-user-doc -->
 * A representation of the literals of the enumeration '<em><b>Preemption Type</b></em>',
 * and utility methods for working with them.
 * <!-- end-user-doc -->
 * @see ecore.tools.vitruv.methodologisttemplate.model.amalthea.AmaltheaPackage#getPreemptionType()
 * @model
 * @generated
 */
public enum PreemptionType implements Enumerator
{
	/**
	 * The '<em><b>Cooperative</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #COOPERATIVE_VALUE
	 * @generated
	 * @ordered
	 */
	COOPERATIVE(0, "cooperative", "cooperative"),

	/**
	 * The '<em><b>Preemptive</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #PREEMPTIVE_VALUE
	 * @generated
	 * @ordered
	 */
	PREEMPTIVE(1, "preemptive", "preemptive");

	/**
	 * The '<em><b>Cooperative</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #COOPERATIVE
	 * @model name="cooperative"
	 * @generated
	 * @ordered
	 */
	public static final int COOPERATIVE_VALUE = 0;

	/**
	 * The '<em><b>Preemptive</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #PREEMPTIVE
	 * @model name="preemptive"
	 * @generated
	 * @ordered
	 */
	public static final int PREEMPTIVE_VALUE = 1;

	/**
	 * An array of all the '<em><b>Preemption Type</b></em>' enumerators.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private static final PreemptionType[] VALUES_ARRAY =
		new PreemptionType[]
		{
			COOPERATIVE,
			PREEMPTIVE,
		};

	/**
	 * A public read-only list of all the '<em><b>Preemption Type</b></em>' enumerators.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static final List<PreemptionType> VALUES = Collections.unmodifiableList(Arrays.asList(VALUES_ARRAY));

	/**
	 * Returns the '<em><b>Preemption Type</b></em>' literal with the specified literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param literal the literal.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static PreemptionType get(String literal)
	{
		for (int i = 0; i < VALUES_ARRAY.length; ++i)
		{
			PreemptionType result = VALUES_ARRAY[i];
			if (result.toString().equals(literal))
			{
				return result;
			}
		}
		return null;
	}

	/**
	 * Returns the '<em><b>Preemption Type</b></em>' literal with the specified name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param name the name.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static PreemptionType getByName(String name)
	{
		for (int i = 0; i < VALUES_ARRAY.length; ++i)
		{
			PreemptionType result = VALUES_ARRAY[i];
			if (result.getName().equals(name))
			{
				return result;
			}
		}
		return null;
	}

	/**
	 * Returns the '<em><b>Preemption Type</b></em>' literal with the specified integer value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the integer value.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static PreemptionType get(int value)
	{
		switch (value)
		{
			case COOPERATIVE_VALUE: return COOPERATIVE;
			case PREEMPTIVE_VALUE: return PREEMPTIVE;
		}
		return null;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private final int value;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private final String name;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private final String literal;

	/**
	 * Only this class can construct instances.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private PreemptionType(int value, String name, String literal)
	{
		this.value = value;
		this.name = name;
		this.literal = literal;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public int getValue()
	{
	  return value;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getName()
	{
	  return name;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getLiteral()
	{
	  return literal;
	}

	/**
	 * Returns the literal value of the enumerator, which is its string representation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String toString()
	{
		return literal;
	}
	
} //PreemptionType
