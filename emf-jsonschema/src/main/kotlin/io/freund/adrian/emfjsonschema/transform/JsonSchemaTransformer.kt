package io.freund.adrian.emfjsonschema.transform

import io.freund.adrian.emfjsonschema.common.schemaToType
import io.freund.adrian.emfjsonschema.schema.JsonSchema
import io.freund.adrian.emfjsonschema.schema.JsonSchemaImpl
import io.freund.adrian.emfjsonschema.schema.JsonSchemaMethod
import io.freund.adrian.emfjsonschema.schema.JsonSchemaParameter
import io.freund.adrian.emfjsonschema.schema.SimpleType
import io.freund.adrian.emfjsonschema.utils.Union
import io.freund.adrian.emfjsonschema.utils.left
import org.eclipse.emf.ecore.EAttribute
import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EClassifier
import org.eclipse.emf.ecore.EEnum
import org.eclipse.emf.ecore.EOperation
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.EParameter
import org.eclipse.emf.ecore.EReference
import org.eclipse.emf.ecore.EStructuralFeature
import org.eclipse.emf.ecore.ETypedElement
import org.eclipse.emf.ecore.EcoreFactory
import org.eclipse.emf.ecore.EcorePackage
import org.eclipse.emf.ecore.resource.ResourceSet
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl

class JsonSchemaTransformer {
    val packages = mutableMapOf<String, EPackage>()
    val classifiers = mutableMapOf<String, EClassifier>()

    init {
        // Load and register built-in ecore types
        val rs: ResourceSet = ResourceSetImpl()
        rs.packageRegistry.put("http://www.eclipse.org/emf/2002/Ecore", EcorePackage.eINSTANCE)
        EcorePackage.eINSTANCE.eClassifiers?.forEach {
            classifiers[classifierToSchemaId(it)] = it
        }
    }

    fun visitSchemata(schemata: List<JsonSchema>): List<EPackage> {
        val classifiers = schemata.map {
            visitTopLevelSchema(it)
        }
        (schemata zip classifiers).forEach {
            visitTopLevelSchemaPass2(it.first, it.second)
        }

        return packages.values.toList()
    }

    fun visitTopLevelSchema(schema: JsonSchema): EClassifier {
        val id = schema.`$id` ?: throw TransformationException("Missing \$id in top-level schema")
        val idPrefix = id.substring(0, id.indexOfLast { it == '/' })

        val pack: EPackage = packages.getOrPut(idPrefix) {
            EcoreFactory.eINSTANCE.createEPackage().also {
                it.nsURI = idPrefix
                it.nsPrefix = schema.namespace
            }
        }
        val classifier = if (schema.enum != null) {
            visitEnumSchema(schema)
        } else if (schema.javaType != null) {
            visitDataTypeSchema(schema)
        } else {
            visitClassSchema(schema)
        }

        classifier.name = schema.title ?: throw TransformationException("Missing `title` in top-level schema")

        pack.eClassifiers.add(classifier)
        this.classifiers[id] = classifier

        return classifier
    }

    fun visitTopLevelSchemaPass2(schema: JsonSchema, classifier: EClassifier) {
        when (classifier) {
            is EClass -> visitClassSchemaPass2(schema, classifier)
        }
    }

    fun visitEnumSchema(schema: JsonSchema): EEnum {
        val entries = schema.enum
        requireNotNull(entries) { "Enum property must not be null" }

        val names = schema.enumNames

        val enum = EcoreFactory.eINSTANCE.createEEnum()

        entries.forEachIndexed { i, literal ->
            if (literal !is String) {
                error { "Only string enums are currently supported" }
            }
            val entry = EcoreFactory.eINSTANCE.createEEnumLiteral()

            if (names != null) {
                entry.name = names[i]
                entry.literal = literal
            } else {
                entry.name = literal
            }
            entry.value = i

            enum.eLiterals.add(entry)
        }

        return enum
    }

    fun visitClassSchema(schema: JsonSchema): EClassifier {
        val clazz = EcoreFactory.eINSTANCE.createEClass()
        clazz.name = schema.title ?: throw Exception("Missing title name in top-level schema")

        clazz.isAbstract = schema.abstract == true
        clazz.isInterface = schema.`interface` == true

        schema.properties?.forEach {
            clazz.eStructuralFeatures.add(visitProperty(it.key, it.value))
        }
        schema.transientProperties?.forEach {
            clazz.eStructuralFeatures.add(visitProperty(it.key, it.value, isTransient = true))
        }
        schema.methods?.forEach {
            clazz.eOperations.add(visitMethod(it.key, it.value))
        }

        return clazz
    }

    fun visitClassSchemaPass2(schema: JsonSchema, clazz: EClass) {
        schema.allOf?.forEach { superSchema ->
            val superClass = classifiers[superSchema.`$ref`]
            if (superClass != null) {
                if (superClass !is EClass) {
                    error { "Only classes can be supertypes" }
                }
                clazz.eSuperTypes.add(superClass)
            }
        }
        schema.properties?.forEach { (name, property) ->
            visitTypedElementPass2(property, clazz.eStructuralFeatures.first { it.name == name })
        }
        schema.transientProperties?.forEach { (name, property) ->
            visitTypedElementPass2(property, clazz.eStructuralFeatures.first { it.name == name })
        }
        /*
        schema.properties?.let { properties ->
            (properties.values zip clazz.eStructuralFeatures).forEach {
                visitTypedElementPass2(it.first, it.second)
            }
        }
         */
        val methods = schema.methods
        if (methods != null) {
            (methods.values zip clazz.eOperations).forEach {
                visitMethodPass2(it.first, it.second)
            }
        }
    }

    fun visitDataTypeSchema(schema: JsonSchema): EClassifier {
        val dataType = EcoreFactory.eINSTANCE.createEDataType()
        val javaType = schema.javaType
        if (javaType != null) {
            dataType.instanceClassName = javaType
            return dataType
        }
        val type = schema.type
        when (type) {
            is Union.Left -> {
                dataType.instanceClass = schemaToType(type.value, schema, nullable = false).java
            }
            is Union.Right -> {
                val union = type.value
                if (union.size > 2 || SimpleType.NULL !in union) {
                    throw NotImplementedError("Union data types are currently not supported.")
                }
                dataType.instanceClass = schemaToType(union.first { it != SimpleType.NULL }, schema, nullable = true).java
            }
            null -> error { "visitDataType must not be called when `type` is null" }
        }

        return dataType
    }

    fun visitProperty(name: String, schema: JsonSchema, isTransient: Boolean = false): EStructuralFeature {
        val feature = if (schema.reference != true) {
            visitAttribute(schema)
        } else {
            visitReference(schema)
        }

        feature.name = name
        if (schema.readOnly == true) {
            feature.isChangeable = false
        }
        if (schema.volatile == true) {
            feature.isVolatile = true
        }
        if (schema.derived == true) {
            feature.isDerived = true
        }
        if (schema.unordered != false) {
            feature.isOrdered = true
        }
        // This doesn't work for complex types.
        if (schema.default != null) {
            schema.default.toString()
        }
        if (isTransient) {
            feature.isTransient = true
        }

        return feature
    }

    fun visitAttribute(schema: JsonSchema): EAttribute {
        val attribute = EcoreFactory.eINSTANCE.createEAttribute()
        if (schema.id == true) {
            attribute.isID = true
        }

        return attribute
    }

    fun visitReference(schema: JsonSchema): EReference {
        val reference = EcoreFactory.eINSTANCE.createEReference()
        if (schema.containment == true) {
            reference.isContainment = true
        }
        if (schema.resolveProxies == true) {
            reference.isResolveProxies = true
        }
        val opposite = schema.opposite

        return reference
    }

    fun visitMethod(name: String, schema: JsonSchemaMethod): EOperation {
        val operation = EcoreFactory.eINSTANCE.createEOperation()
        operation.name = name

        schema.parameters?.forEach {
            operation.eParameters.add(visitParameter(it))
        }

        return operation
    }

    fun visitMethodPass2(schema: JsonSchemaMethod, operation: EOperation) {
        val returnType = schema.returnType
        if (returnType != null) {
            visitTypedElementPass2(returnType, operation)
        }

        (schema.parameters?.zip(operation.eParameters))?.forEach { (schema, parameter) ->
            visitTypedElementPass2(schema, parameter)
        }
    }

    fun visitParameter(schema: JsonSchemaParameter): EParameter {
        val parameter = EcoreFactory.eINSTANCE.createEParameter()
        parameter.name = schema.argumentName ?: throw TransformationException("Parameter missing name")

        return parameter
    }

    fun visitTypedElementPass2(schema: JsonSchema, typedElement: ETypedElement) {
        val typeSchema = if (schema.type == SimpleType.ARRAY.left()) {
            typedElement.lowerBound = schema.minItems ?: 0
            typedElement.upperBound = schema.maxItems ?: ETypedElement.UNBOUNDED_MULTIPLICITY
            typedElement.isUnique = schema.uniqueItems == true

            schema.items ?: JsonSchemaImpl()
        } else {
            schema
        }

        val ref = typeSchema.`$ref`
        requireNotNull(ref)

        val genericType = EcoreFactory.eINSTANCE.createEGenericType()
        val classifier = schemaIdToClassifier(ref, classifiers)

        val defs = schema.`$defs`
        if (defs != null) {
            classifier.eTypeParameters.forEach { parameter ->
                if (parameter.name in defs.keys) {
                    val def = defs[parameter.name]!!
                    require(def.`$dynamicAnchor` == parameter.name)

                    val typeArgument = EcoreFactory.eINSTANCE.createEGenericType()
                    val ref = def.`$ref`
                    if (ref != null) {
                        typeArgument.eClassifier = schemaIdToClassifier(ref, classifiers)
                    }

                    genericType.eTypeArguments.add(typeArgument)
                }
            }
        }

        genericType.eClassifier = classifier

        typedElement.eGenericType = genericType

        if (typedElement is EReference) {
            val opposite = schema.opposite
            if (opposite != null) {
                typedElement.eOpposite = schemaIdToReference(opposite, classifiers)
            }
        }
    }
}

class TransformationException(msg: String) : Exception(msg)
