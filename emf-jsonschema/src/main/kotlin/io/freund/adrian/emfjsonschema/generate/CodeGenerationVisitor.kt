package io.freund.adrian.emfjsonschema.generate

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.typeNameOf
import io.freund.adrian.emfjsonschema.EMF_JSONSCHEMA_ID_PREFIX
import io.freund.adrian.emfjsonschema.common.schemaToType
import io.freund.adrian.emfjsonschema.schema.JsonSchema
import io.freund.adrian.emfjsonschema.schema.JsonSchemaMethod
import io.freund.adrian.emfjsonschema.schema.SimpleType
import io.freund.adrian.emfjsonschema.schema.isPureReference
import io.freund.adrian.emfjsonschema.utils.Union
import io.freund.adrian.emfjsonschema.utils.left
import org.eclipse.emf.common.util.DiagnosticChain
import org.eclipse.emf.ecore.EObject
import java.math.BigDecimal
import java.math.BigInteger

object JsonNull

class CodeGenerationVisitor {
    val types = mutableMapOf<String, TypeName>(
        "$EMF_JSONSCHEMA_ID_PREFIX/types/EBigDecimal.json" to typeNameOf<BigDecimal?>(),
        "$EMF_JSONSCHEMA_ID_PREFIX/types/EBigInteger.json" to typeNameOf<BigInteger?>(),
        "$EMF_JSONSCHEMA_ID_PREFIX/types/EBoolean.json" to typeNameOf<Boolean>(),
        "$EMF_JSONSCHEMA_ID_PREFIX/types/EBooleanObject.json" to typeNameOf<Boolean?>(),
        "$EMF_JSONSCHEMA_ID_PREFIX/types/EChar.json" to typeNameOf<Char>(),
        "$EMF_JSONSCHEMA_ID_PREFIX/types/ECharacterObject.json" to typeNameOf<Char?>(),
        "$EMF_JSONSCHEMA_ID_PREFIX/types/EDouble.json" to typeNameOf<Double>(),
        "$EMF_JSONSCHEMA_ID_PREFIX/types/EDoubleObject.json" to typeNameOf<Double?>(),
        "$EMF_JSONSCHEMA_ID_PREFIX/types/EFloat.json" to typeNameOf<Float>(),
        "$EMF_JSONSCHEMA_ID_PREFIX/types/EFloatObject.json" to typeNameOf<Float?>(),
        "$EMF_JSONSCHEMA_ID_PREFIX/types/EInt.json" to typeNameOf<Int>(),
        "$EMF_JSONSCHEMA_ID_PREFIX/types/EIntegerObject.json" to typeNameOf<Int?>(),
        "$EMF_JSONSCHEMA_ID_PREFIX/types/EEList.json" to List::class.asClassName(),
        "$EMF_JSONSCHEMA_ID_PREFIX/types/ELong.json" to typeNameOf<Long>(),
        "$EMF_JSONSCHEMA_ID_PREFIX/types/ELongObject.json" to typeNameOf<Long?>(),
        "$EMF_JSONSCHEMA_ID_PREFIX/types/EMap.json" to Map::class.asClassName(),
        "$EMF_JSONSCHEMA_ID_PREFIX/types/EShort.json" to typeNameOf<Short>(),
        "$EMF_JSONSCHEMA_ID_PREFIX/types/EShortObject.json" to typeNameOf<Short?>(),
        "$EMF_JSONSCHEMA_ID_PREFIX/types/EDiagnosticChain.json" to typeNameOf<DiagnosticChain?>(),
        "$EMF_JSONSCHEMA_ID_PREFIX/types/EString.json" to typeNameOf<String?>(),
        "$EMF_JSONSCHEMA_ID_PREFIX/types/EObject.json" to typeNameOf<EObject>(),
    )

    fun visitSchemata(schemata: Map<String, JsonSchema>): List<FileSpec> {
        schemata.forEach {
            registerTopLevelSchema(it.key, it.value)
        }

        val classifiers = schemata.mapNotNull {
            visitTopLevelSchema(it.key, it.value)
        }

        return classifiers
    }

    fun registerTopLevelSchema(filename: String, schema: JsonSchema) {
        val id = schema.`$id`
        if (id == null) {
            return
        }
        // Known java type
        schema.javaType?.let {
            types[id] = javaTypeToTypeName(it)
            return
        }
        // Type specified in JSON-Schema
        val type = schema.type
        if (type is Union.Left<SimpleType> && type.value != SimpleType.OBJECT) {
            val kotlinType = schemaToType(type.value, schema, nullable = false)
            types[id] = kotlinType.asTypeName()
            return
        }

        types[id] = ClassName(schema.namespace ?: "", schema.title ?: filename)
    }

    fun visitTopLevelSchema(filename: String, schema: JsonSchema): FileSpec? {
        // No need to generate a class. We just reference the existing class.
        if (schema.javaType != null) {
            return null
        }

        val file = FileSpec.builder(schema.namespace ?: "", schema.title ?: filename)

        val classifier = if (schema.enum != null) {
            visitEnumSchema(filename, schema)
        } else {
            visitClassSchema(filename, schema)
        }
        file.addType(classifier)

        return file.build()
    }

    fun visitEnumSchema(filename: String, schema: JsonSchema): TypeSpec {
        val enumBuilder = TypeSpec.enumBuilder(schema.title ?: filename)
        val enumValues = schema.enum ?: error { "This function must not be called when enum is null" }

        val valueType = if (enumValues.all { it is String }) {
            String::class
        } else if (enumValues.all { it is Int }) {
            Int::class
        } else {
            error { "Only string and integer enums are supported" }
        }

        val enumNames = schema.enumNames
        // Simple enum without values
        if (enumNames == null) {
            if (valueType != String::class) {
                error { "Can't generate non-string enum without names" }
            }
            enumValues.forEach {
                enumBuilder.addEnumConstant(it as String)
            }
        }
        // Complex enum with string or int values
        else {
            enumBuilder.primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter("literal", valueType)
                    .build(),
            )
            enumNames.zip(enumValues).forEach {
                val classBuilder = TypeSpec.anonymousClassBuilder()
                when (valueType) {
                    String::class -> classBuilder.addSuperclassConstructorParameter("%S", it.second as String)
                    Int::class -> classBuilder.addSuperclassConstructorParameter("%L", it.second as Int)
                }
                enumBuilder.addEnumConstant(it.first, classBuilder.build())
            }
        }

        return enumBuilder.build()
    }

    fun visitClassSchema(filename: String, schema: JsonSchema): TypeSpec {
        val interfaceBuilder = TypeSpec.interfaceBuilder(ClassName(schema.namespace ?: "", schema.title ?: filename))

        schema.allOf?.forEach {
            val ref = if (!it.isPureReference()) {
                error { "Anonymous supertypes are not allowed" }
            } else {
                it.`$ref`!!
            }
            interfaceBuilder.addSuperinterface(
                types.getOrElse(ref) {
                    println("Unknown type: $ref")
                    typeNameOf<Any>()
                },
            )
        }

        schema.properties?.forEach {
            interfaceBuilder.addProperty(visitProperty(it.key, it.value, schema.required?.contains(it.key) == true))
        }
        schema.transientProperties?.forEach {
            interfaceBuilder.addProperty(visitProperty(it.key, it.value, schema.required?.contains(it.key) == true, transient = true))
        }
        schema.methods?.forEach {
            interfaceBuilder.addFunction(visitMethod(it.key, it.value))
        }

        return interfaceBuilder.build()
    }

    fun visitProperty(name: String, schema: JsonSchema, required: Boolean, transient: Boolean = false): PropertySpec {
        var type = if (schema.type == SimpleType.ARRAY.left()) {
            val itemsSchema = schema.items

            if (itemsSchema != null) {
                List::class.asTypeName().parameterizedBy(getType(itemsSchema))
            } else {
                typeNameOf<List<Any>>()
            }
        } else {
            if (schema.isPureReference()) {
                types.getOrElse(schema.`$ref`!!) {
                    error("Unknown type: ${schema.`$ref`}")
                }
            } else {
                getType(schema)
            }
        }
        if (!required) {
            type = type.copy(nullable = true)
        }
        val propertyBuilder = PropertySpec.builder(name, type)

        if (schema.readOnly != true) {
            propertyBuilder.mutable()
        }

        return propertyBuilder.build()
    }

    fun visitMethod(name: String, schema: JsonSchemaMethod): FunSpec {
        val functionBuilder = FunSpec.builder(name)
        functionBuilder.addModifiers(KModifier.ABSTRACT)

        val returnType = schema.returnType
        if (returnType != null) {
            functionBuilder.returns(getType(returnType))
        }
        schema.parameters?.forEachIndexed { i, parameter ->
            functionBuilder.addParameter(parameter.title ?: "arg$i", getType(parameter))
        }

        return functionBuilder.build()
    }

    fun getType(jsonSchema: JsonSchema): TypeName {
        jsonSchema.`$ref`?.let {
            return types.getOrElse(it) {
                println("Unknown type: $it")
                typeNameOf<Any>()
            }
        }
        val type = jsonSchema.type
        return when (type) {
            is Union.Left -> schemaToType(type.value, jsonSchema, nullable = false).asTypeName()
            is Union.Right -> {
                var union = type.value
                if (union.size > 2 || SimpleType.NULL !in union) {
                    error("Union data types are not supported.")
                }
                val type = schemaToType(union.first { it != SimpleType.NULL }, jsonSchema, nullable = false).asTypeName()
                type.copy(nullable = true)
            }
            null -> typeNameOf<Any>()
        }
    }

    fun javaTypeToTypeName(type: String): TypeName = when (type) {
        "byte" -> typeNameOf<Byte>()
        "byte[]" -> typeNameOf<ByteArray>()
        "short" -> typeNameOf<Short>()
        "short[]" -> typeNameOf<ShortArray>()
        "int" -> typeNameOf<Int>()
        "int[]" -> typeNameOf<IntArray>()
        "long" -> typeNameOf<Long>()
        "long[]" -> typeNameOf<LongArray>()
        "float" -> typeNameOf<Float>()
        "float[]" -> typeNameOf<FloatArray>()
        "double" -> typeNameOf<Double>()
        "double[]" -> typeNameOf<DoubleArray>()
        "boolean" -> typeNameOf<Boolean>()
        "boolean[]" -> typeNameOf<BooleanArray>()
        "char" -> typeNameOf<Char>()
        "char[]" -> typeNameOf<CharArray>()
        else -> {
            val lastSep = type.lastIndexOf('.')
            val packageName = type.substring(0, lastSep)
            val className = type.substring(lastSep + 1, type.length)
            ClassName(packageName, className)
        }
    }
}
