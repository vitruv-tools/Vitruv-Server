package io.freund.adrian.emfjsonschema.transform

import io.freund.adrian.emfjsonschema.EMF_JSONSCHEMA_ID_PREFIX
import io.freund.adrian.emfjsonschema.schema.JsonSchemaImpl
import io.freund.adrian.emfjsonschema.schema.JsonSchemaMethodImpl
import io.freund.adrian.emfjsonschema.schema.JsonSchemaParameterImpl
import io.freund.adrian.emfjsonschema.schema.MutableJsonSchema
import io.freund.adrian.emfjsonschema.schema.MutableJsonSchemaMethod
import io.freund.adrian.emfjsonschema.schema.MutableJsonSchemaParameter
import io.freund.adrian.emfjsonschema.schema.SimpleType
import io.freund.adrian.emfjsonschema.utils.left
import io.freund.adrian.emfjsonschema.utils.right
import io.freund.adrian.emfjsonschema.utils.toBigDecimal
import org.eclipse.emf.ecore.EAttribute
import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EClassifier
import org.eclipse.emf.ecore.EDataType
import org.eclipse.emf.ecore.EEnum
import org.eclipse.emf.ecore.EGenericType
import org.eclipse.emf.ecore.EOperation
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.EParameter
import org.eclipse.emf.ecore.EReference
import org.eclipse.emf.ecore.EStructuralFeature
import org.eclipse.emf.ecore.ETypedElement
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.collections.associateBy

/**
 * Transforms an Ecore Meta-Model into a JSON-Schema schema
 */
class EcoreTransformer {

    /**
     * An EPackage becomes a directory of JSON-Schema schemata
     *
     * @param pack The package to transform
     * @return A map of filenames to schemata
     */
    fun visitPackage(pack: EPackage): Map<String, MutableJsonSchema> {
        val classifiers = pack.eClassifiers.associateBy({
            "${it.name}.json"
        }, {
            this.visitClassifier(it)
        }).toMutableMap()

        pack.eSubpackages.forEach { subpack ->
            val subclassifiers = visitPackage(subpack)
            subclassifiers.forEach { classifier ->
                classifiers["${subpack.name}/${classifier.key}"] = classifier.value
            }
        }

        return classifiers
    }

    fun visitClassifier(classifier: EClassifier): MutableJsonSchema {
        val schema = when (classifier) {
            is EClass -> visitClass(classifier)
            is EEnum -> visitEnum(classifier)
            is EDataType -> visitDataType(classifier)
            else -> error("Missing case for classifier ${classifier.javaClass.name}")
        }
        schema.title = classifier.name
        schema.`$id` = classifierToSchemaId(classifier)
        schema.namespace = classifier.ePackage.nsPrefix
        schema.`$schema` = "${EMF_JSONSCHEMA_ID_PREFIX}/schema.json"

        return schema
    }

    fun visitClass(clazz: EClass): MutableJsonSchema {
        val schema = JsonSchemaImpl()
        schema.type = mutableListOf(SimpleType.OBJECT, SimpleType.NULL).right()
        if (clazz.isAbstract) {
            schema.abstract = true
        }
        if (clazz.isInterface) {
            schema.`interface` = true
        }

        schema.properties = clazz.eStructuralFeatures.filter { !it.isTransient }
            .associateBy({
                it.name
            }, {
                this.visitStructuralFeature(it)
            }).toMutableMap()
        // Transient properties are like properties, but not persisted.
        schema.transientProperties = clazz.eStructuralFeatures.filter { it.isTransient }
            .associateBy({
                it.name
            }, {
                this.visitStructuralFeature(it)
            }).toMutableMap()

        schema.required = clazz.eStructuralFeatures.filter { !it.isUnsettable }.map { it.name }.toMutableList()

        schema.methods = clazz.eOperations.associateBy({ it.name }, {
            this.visitOperation(it)
        }).toMutableMap()

        // If we only have one superType we can use `$ref` directly. If we have multiple we need to use `allOf`
        if (clazz.eSuperTypes.size == 1 && clazz.eGenericSuperTypes.isEmpty()) {
            schema.`$ref` = classifierToSchemaId(clazz.eSuperTypes.first())
        } else if (clazz.eSuperTypes.size + clazz.eGenericSuperTypes.size != 0) {
            schema.allOf = clazz.eGenericSuperTypes.map {
                val schema = JsonSchemaImpl()
                handleGenericType(schema, it)
                schema
            }.toMutableList()
        }

        return schema
    }

    fun visitStructuralFeature(structuralFeature: EStructuralFeature): MutableJsonSchema {
        val schema = JsonSchemaImpl()
        handleTypedElement(schema, structuralFeature)

        /* We use `if` instead of direct assignment to make
         * use of the default values and keep the generated schema more readable.
         */
        if (!structuralFeature.isChangeable) {
            schema.readOnly = true
        }
        if (structuralFeature.isVolatile) {
            schema.volatile = true
        }
        if (structuralFeature.isDerived) {
            schema.derived = true
        }

        // TODO: Parse the literal
        schema.default = structuralFeature.defaultValueLiteral

        when (structuralFeature) {
            is EAttribute -> {
                if (structuralFeature.isID) {
                    schema.id = true
                }
            }
            is EReference -> {
                schema.reference = true
                if (structuralFeature.isContainment) {
                    schema.containment = true
                }
                if (!structuralFeature.isResolveProxies) {
                    schema.resolveProxies = false
                }

                val opposite = structuralFeature.eOpposite
                if (opposite != null) {
                    schema.opposite = referenceToSchemaId(opposite)
                }
            }
        }

        return schema
    }

    fun visitEnum(enum: EEnum): MutableJsonSchema {
        val schema = JsonSchemaImpl()

        val literals = enum.eLiterals.sortedBy { it.value }

        schema.enum = literals.map { it.literal ?: it.name }.toMutableList()
        if (literals.any { it.literal != null }) {
            schema.enumNames = literals.map { it.name }.toMutableList()
        }

        return schema
    }

    fun visitDataType(dataType: EDataType): MutableJsonSchema {
        val schema =
            when (dataType.instanceClass) {
                Char::class.java -> JsonSchemaImpl(
                    type = SimpleType.STRING.left(),
                    minLength = 1,
                    maxLength = 1,
                )
                java.lang.Character::class.java -> JsonSchemaImpl(
                    type = mutableListOf(SimpleType.STRING, SimpleType.NULL).right(),
                    minLength = 1,
                    maxLength = 1,
                )
                String::class.java -> JsonSchemaImpl(
                    type = mutableListOf(SimpleType.STRING, SimpleType.NULL).right(),
                )
                Boolean::class.java -> JsonSchemaImpl(
                    type = SimpleType.BOOLEAN.left(),
                )
                java.lang.Boolean::class.java -> JsonSchemaImpl(
                    type = mutableListOf(SimpleType.BOOLEAN, SimpleType.NULL).right(),
                )
                Byte::class.java -> JsonSchemaImpl(
                    type = SimpleType.INTEGER.left(),
                    minimum = Byte.MIN_VALUE.toBigDecimal(),
                    maximum = Byte.MAX_VALUE.toBigDecimal(),
                )
                java.lang.Byte::class.java -> JsonSchemaImpl(
                    type = mutableListOf(SimpleType.INTEGER, SimpleType.NULL).right(),
                    minimum = Byte.MIN_VALUE.toBigDecimal(),
                    maximum = Byte.MAX_VALUE.toBigDecimal(),
                )
                ByteArray::class.java -> JsonSchemaImpl(
                    type = mutableListOf(SimpleType.ARRAY, SimpleType.NULL).right(),
                    items = JsonSchemaImpl(
                        type = SimpleType.INTEGER.left(),
                        minimum = Byte.MIN_VALUE.toBigDecimal(),
                        maximum = Byte.MAX_VALUE.toBigDecimal(),
                    ),
                )
                Short::class.java -> JsonSchemaImpl(
                    type = SimpleType.INTEGER.left(),
                    minimum = Short.MIN_VALUE.toBigDecimal(),
                    maximum = Short.MAX_VALUE.toBigDecimal(),
                )
                java.lang.Short::class.java -> JsonSchemaImpl(
                    type = mutableListOf(SimpleType.INTEGER, SimpleType.NULL).right(),
                    minimum = Short.MIN_VALUE.toBigDecimal(),
                    maximum = Short.MAX_VALUE.toBigDecimal(),
                )
                Int::class.java -> JsonSchemaImpl(
                    type = SimpleType.INTEGER.left(),
                    minimum = Int.MIN_VALUE.toBigDecimal(),
                    maximum = Int.MAX_VALUE.toBigDecimal(),
                )
                java.lang.Integer::class.java -> JsonSchemaImpl(
                    type = mutableListOf(SimpleType.INTEGER, SimpleType.NULL).right(),
                    minimum = Int.MIN_VALUE.toBigDecimal(),
                    maximum = Int.MAX_VALUE.toBigDecimal(),
                )
                Long::class.java -> JsonSchemaImpl(
                    type = SimpleType.INTEGER.left(),
                    minimum = Long.MIN_VALUE.toBigDecimal(),
                    maximum = Long.MAX_VALUE.toBigDecimal(),
                )
                java.lang.Long::class.java -> JsonSchemaImpl(
                    type = mutableListOf(SimpleType.INTEGER, SimpleType.NULL).right(),
                    minimum = Long.MIN_VALUE.toBigDecimal(),
                    maximum = Long.MAX_VALUE.toBigDecimal(),
                )
                BigInteger::class.java -> JsonSchemaImpl(
                    type = mutableListOf(SimpleType.INTEGER, SimpleType.NULL).right(),
                )
                Float::class.java, Double::class.java -> JsonSchemaImpl(
                    type = SimpleType.NUMBER.left(),
                )
                java.lang.Float::class.java, java.lang.Double::class.java, BigDecimal::class.java -> JsonSchemaImpl(
                    type = mutableListOf(SimpleType.NUMBER, SimpleType.NULL).right(),
                )
                Void::class.java -> JsonSchemaImpl(
                    type = SimpleType.NULL.left(),
                )
                java.util.Date::class.java -> JsonSchemaImpl(
                    type = mutableListOf(SimpleType.STRING, SimpleType.NULL).right(),
                    format = "date-time",
                )
                else -> JsonSchemaImpl() // No special handling for this type implemented
            }

        schema.javaType = dataType.instanceClassName

        return schema
    }

    fun visitOperation(operation: EOperation): MutableJsonSchemaMethod {
        val method = JsonSchemaMethodImpl()

        if (operation.eType != null) {
            method.returnType = JsonSchemaImpl().also { handleGenericType(it, operation.eGenericType) }
        }
        method.parameters = operation.eParameters.map { this.visitParameter(it) }
        return method
    }

    fun visitParameter(parameter: EParameter): MutableJsonSchemaParameter {
        val schema = JsonSchemaParameterImpl()
        handleTypedElement(schema, parameter)

        schema.argumentName = parameter.name

        return schema
    }

    fun handleTypedElement(schema: MutableJsonSchema, typedElement: ETypedElement) {
        if (typedElement.isMany) {
            // types with `isMany` set need toe be represented as arrays in JSON-Schema.
            schema.type = SimpleType.ARRAY.left()
            if (typedElement.lowerBound != 0) {
                schema.minItems = typedElement.lowerBound
            }
            if (typedElement.upperBound != ETypedElement.UNBOUNDED_MULTIPLICITY) {
                schema.maxItems = typedElement.upperBound
            }
            if (typedElement.isUnique) {
                schema.uniqueItems = true
            }
            if (!typedElement.isOrdered) {
                schema.unordered = true
            }

            schema.items = JsonSchemaImpl().also {
                handleGenericType(it, typedElement.eGenericType)
            }
        } else {
            handleGenericType(schema, typedElement.eGenericType)
        }
    }

    private fun handleGenericType(schema: MutableJsonSchema, genericType: EGenericType) {
        val typeArguments = genericType.eTypeArguments
        if (typeArguments != null && typeArguments.isNotEmpty()) {
            schema.`$defs` = typeArguments.mapIndexed { i, arg ->
                val name = genericType.eClassifier.eTypeParameters[i].name
                val schema: MutableJsonSchema = JsonSchemaImpl(`$dynamicAnchor` = name).also {
                    val classifier = arg.eClassifier
                    if (classifier != null) {
                        it.`$ref` = classifierToSchemaId(arg.eClassifier)
                    }
                }

                name to schema
            }.toMap().toMutableMap()
        }
        if (genericType.eClassifier != null) {
            schema.`$ref` = classifierToSchemaId(genericType.eClassifier)
        }
    }
}
