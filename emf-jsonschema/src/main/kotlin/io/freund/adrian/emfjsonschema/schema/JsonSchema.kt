@file:Suppress(
    "UNUSED_PROPERTY",
    "ktlint:standard:property-naming",
)

package io.freund.adrian.emfjsonschema.schema

/*
 * This whole file would ideally be generated from the JSON-Schema meta-schema, but we're not at that point yet.
 */

import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import io.freund.adrian.emfjsonschema.utils.Union
import java.math.BigDecimal

typealias AnchorString = String
typealias UriString = String
typealias UriReferenceString = String

enum class SimpleType(
    @JsonValue val value: String,
) {
    ARRAY("array"),
    BOOLEAN("boolean"),
    INTEGER("integer"),
    NULL("null"),
    NUMBER("number"),
    OBJECT("object"),
    STRING("string"),
}

@JsonDeserialize(`as` = JsonSchemaImpl::class)
interface JsonSchema {
    // meta/core
    val `$id`: UriReferenceString?
    val `$schema`: UriString?
    val `$ref`: UriReferenceString?
    val `$anchor`: AnchorString?
    val `$dynamicRef`: UriReferenceString?
    val `$dynamicAnchor`: AnchorString?
    val `$vocabulary`: Map<UriString, Boolean>?
    val `$comment`: String?
    val `$defs`: Map<String, JsonSchema>?

    // meta/applicator
    val prefixItems: List<JsonSchema>?
    val items: JsonSchema?
    val contains: JsonSchema?
    val additionalProperties: JsonSchema?
    val properties: Map<String, JsonSchema>?
    val patternProperties: Map<Regex, JsonSchema>?
    val dependentSchemas: Map<String, JsonSchema>?
    val propertyNames: JsonSchema?
    val `if`: JsonSchema?
    val then: JsonSchema?
    val `else`: JsonSchema?
    val allOf: List<JsonSchema>?
    val anyOf: List<JsonSchema>?
    val oneOf: List<JsonSchema>?
    val not: JsonSchema?

    // meta/unevaluated
    val unevaluatedItems: JsonSchema?
    val unevaluatedProperties: JsonSchema?

    // meta/validation
    val type: Union<SimpleType, List<SimpleType>>?
    val enum: List<*>?
    val multipleOf: Int?
    val maximum: BigDecimal?
    val exclusiveMaximum: BigDecimal?
    val minimum: BigDecimal?
    val exclusiveMinimum: BigDecimal?
    val maxLength: Int?
    val minLength: Int?
    val pattern: Regex?
    val maxItems: Int?
    val minItems: Int?
    val uniqueItems: Boolean?
    val maxContains: Int?
    val minContains: Int?
    val maxProperties: Int?
    val minProperties: Int?
    val required: List<String>?
    val dependentRequired: Map<String, List<String>>?

    // meta/meta-data
    val title: String?
    val description: String?
    val deprecated: Boolean?
    val readOnly: Boolean?
    val writeOnly: Boolean?
    val examples: List<String>?
    val default: Any? // `default` is missing from the meta-schema, but described in the vocabulary.

    // meta/format-annotation
    val format: String?

    // meta/content
    val contentEncoding: String?
    val contentMediaType: String?
    val contentSchema: JsonSchema?

    // emf-jsonschema/java-type
    val javaType: String?

    // emf-jsonschema/package
    val namespace: String?

    // emf-jsonschema/class
    val abstract: Boolean?
    var `interface`: Boolean?

    // emf-jsonschema/enum
    val enumNames: List<String>?
    val enumIndices: List<Int>?

    // emf-jsonschema/structural-features
    val volatile: Boolean?
    val derived: Boolean?
    var unordered: Boolean?
    val transientProperties: MutableMap<String, JsonSchema>?
    val reference: Boolean?

    // emf-jsonschema/attributes
    val id: Boolean?

    // emf-jsonschema/references
    val containment: Boolean?
    val resolveProxies: Boolean?
    val opposite: String?

    // emf-jsonschema/methods
    val methods: Map<String, JsonSchemaMethod>?
}

@JsonDeserialize(`as` = JsonSchemaImpl::class)
interface MutableJsonSchema : JsonSchema {
    // meta/core
    override var `$id`: UriReferenceString?
    override var `$schema`: UriString?
    override var `$ref`: UriReferenceString?
    override var `$anchor`: AnchorString?
    override var `$dynamicRef`: UriReferenceString?
    override var `$dynamicAnchor`: AnchorString?
    override var `$vocabulary`: MutableMap<UriString, Boolean>?
    override var `$comment`: String?
    override var `$defs`: MutableMap<String, MutableJsonSchema>?

    // meta/applicator
    override var prefixItems: MutableList<MutableJsonSchema>?
    override var items: MutableJsonSchema?
    override var contains: MutableJsonSchema?
    override var additionalProperties: MutableJsonSchema?
    override var properties: MutableMap<String, MutableJsonSchema>?
    override var patternProperties: MutableMap<Regex, MutableJsonSchema>?
    override var dependentSchemas: MutableMap<String, MutableJsonSchema>?
    override var propertyNames: MutableJsonSchema?
    override var `if`: MutableJsonSchema?
    override var then: MutableJsonSchema?
    override var `else`: MutableJsonSchema?
    override var allOf: MutableList<MutableJsonSchema>?
    override var anyOf: MutableList<MutableJsonSchema>?
    override var oneOf: MutableList<MutableJsonSchema>?
    override var not: MutableJsonSchema?

    // meta/unevaluated
    override var unevaluatedItems: MutableJsonSchema?
    override var unevaluatedProperties: MutableJsonSchema?

    // meta/validation
    override var type: Union<SimpleType, MutableList<SimpleType>>?
    override var enum: MutableList<*>?
    override var multipleOf: Int?
    override var maximum: BigDecimal?
    override var exclusiveMaximum: BigDecimal?
    override var minimum: BigDecimal?
    override var exclusiveMinimum: BigDecimal?
    override var maxLength: Int?
    override var minLength: Int?
    override var pattern: Regex?
    override var maxItems: Int?
    override var minItems: Int?
    override var uniqueItems: Boolean?
    override var maxContains: Int?
    override var minContains: Int?
    override var maxProperties: Int?
    override var minProperties: Int?
    override var required: MutableList<String>?
    override var dependentRequired: MutableMap<String, MutableList<String>>?

    // meta/meta-data
    override var title: String?
    override var description: String?
    override var deprecated: Boolean?
    override var readOnly: Boolean?
    override var writeOnly: Boolean?
    override var examples: MutableList<String>?
    override var default: Any?

    // meta/format-annotation
    override var format: String?

    // meta/content
    override var contentEncoding: String?
    override var contentMediaType: String?
    override var contentSchema: MutableJsonSchema?

    // emf-jsonschema/java-type
    override var javaType: String?

    // emf-jsonschema/package
    override var namespace: String?

    // emf-jsonschema/class
    override var abstract: Boolean?
    override var `interface`: Boolean?

    // emf-jsonschema/enum
    override var enumNames: List<String>?
    override var enumIndices: List<Int>?

    // emf-jsonschema/structural-feature
    override var volatile: Boolean?
    override var derived: Boolean?
    override var unordered: Boolean?
    override var transientProperties: MutableMap<String, JsonSchema>?
    override var reference: Boolean?

    // emf-jsonschema/attribute
    override var id: Boolean?

    // emf-jsonschema/reference
    override var containment: Boolean?
    override var resolveProxies: Boolean?
    override var opposite: String?

    // emf-jsonschema/method
    override var methods: MutableMap<String, MutableJsonSchemaMethod>?
}

data class JsonSchemaImpl(
    // meta/core
    override var `$id`: UriReferenceString? = null,
    override var `$schema`: UriString? = null,
    override var `$ref`: UriReferenceString? = null,
    override var `$anchor`: AnchorString? = null,
    override var `$dynamicRef`: UriReferenceString? = null,
    override var `$dynamicAnchor`: AnchorString? = null,
    override var `$vocabulary`: MutableMap<UriString, Boolean>? = null,
    override var `$comment`: String? = null,
    override var `$defs`: MutableMap<String, MutableJsonSchema>? = null,

    // meta/applicator
    override var prefixItems: MutableList<MutableJsonSchema>? = null,
    override var items: MutableJsonSchema? = null,
    override var contains: MutableJsonSchema? = null,
    override var additionalProperties: MutableJsonSchema? = null,
    override var properties: MutableMap<String, MutableJsonSchema>? = null,
    override var patternProperties: MutableMap<Regex, MutableJsonSchema>? = null,
    override var dependentSchemas: MutableMap<String, MutableJsonSchema>? = null,
    override var propertyNames: MutableJsonSchema? = null,
    override var `if`: MutableJsonSchema? = null,
    override var then: MutableJsonSchema? = null,
    override var `else`: MutableJsonSchema? = null,
    override var allOf: MutableList<MutableJsonSchema>? = null,
    override var anyOf: MutableList<MutableJsonSchema>? = null,
    override var oneOf: MutableList<MutableJsonSchema>? = null,
    override var not: MutableJsonSchema? = null,

    // meta/unevaluated
    override var unevaluatedItems: MutableJsonSchema? = null,
    override var unevaluatedProperties: MutableJsonSchema? = null,

    // meta/validation
    override var type: Union<SimpleType, MutableList<SimpleType>>? = null,
    override var enum: MutableList<*>? = null,
    override var multipleOf: Int? = null,
    override var maximum: BigDecimal? = null,
    override var exclusiveMaximum: BigDecimal? = null,
    override var minimum: BigDecimal? = null,
    override var exclusiveMinimum: BigDecimal? = null,
    override var maxLength: Int? = null,
    override var minLength: Int? = null,
    override var pattern: Regex? = null,
    override var maxItems: Int? = null,
    override var minItems: Int? = null,
    override var uniqueItems: Boolean? = null,
    override var maxContains: Int? = null,
    override var minContains: Int? = null,
    override var maxProperties: Int? = null,
    override var minProperties: Int? = null,
    override var required: MutableList<String>? = null,
    override var dependentRequired: MutableMap<String, MutableList<String>>? = null,

    // meta/meta-data
    override var title: String? = null,
    override var description: String? = null,
    override var deprecated: Boolean? = null,
    override var readOnly: Boolean? = null,
    override var writeOnly: Boolean? = null,
    override var examples: MutableList<String>? = null,
    override var default: Any? = null,

    // meta/format-annotation
    override var format: String? = null,

    // meta/content
    override var contentEncoding: String? = null,
    override var contentMediaType: String? = null,
    override var contentSchema: MutableJsonSchema? = null,

    // emf-jsonschema/java-type
    override var javaType: String? = null,

    // emf-jsonschema/package
    override var namespace: String? = null,

    // emf-jsonschema/class
    override var abstract: Boolean? = null,
    override var `interface`: Boolean? = null,

    // emf-jsonschema/enum
    override var enumNames: List<String>? = null,
    override var enumIndices: List<Int>? = null,

    // emf-jsonschema/structural-features
    override var volatile: Boolean? = null,
    override var derived: Boolean? = null,
    override var unordered: Boolean? = null,
    override var transientProperties: MutableMap<String, JsonSchema>? = null,
    override var reference: Boolean? = null,

    // emf-jsonschema/attributes
    override var id: Boolean? = null,

    // emf-jsonschema/references
    override var containment: Boolean? = null,
    override var resolveProxies: Boolean? = null,
    override var opposite: String? = null,

    // emf-jsonschema/methods
    override var methods: MutableMap<String, MutableJsonSchemaMethod>? = null,
) : MutableJsonSchema

@JsonDeserialize(`as` = JsonSchemaMethodImpl::class)
interface JsonSchemaMethod {
    val parameters: List<JsonSchemaParameter>?
    val returnType: JsonSchema?
}

@JsonDeserialize(`as` = JsonSchemaMethodImpl::class)
interface MutableJsonSchemaMethod : JsonSchemaMethod {
    override var parameters: List<JsonSchemaParameter>?
    override var returnType: JsonSchema?
}

data class JsonSchemaMethodImpl(
    override var parameters: List<JsonSchemaParameter>? = null,
    override var returnType: JsonSchema? = null,
) : MutableJsonSchemaMethod

@JsonDeserialize(`as` = JsonSchemaParameterImpl::class)
interface JsonSchemaParameter : JsonSchema {
    val argumentName: String?
}

@JsonDeserialize(`as` = JsonSchemaParameterImpl::class)
interface MutableJsonSchemaParameter :
    JsonSchemaParameter,
    MutableJsonSchema {
    override var argumentName: String?
}

data class JsonSchemaParameterImpl(
    // meta/core
    override var `$id`: UriReferenceString? = null,
    override var `$schema`: UriString? = null,
    override var `$ref`: UriReferenceString? = null,
    override var `$anchor`: AnchorString? = null,
    override var `$dynamicRef`: UriReferenceString? = null,
    override var `$dynamicAnchor`: AnchorString? = null,
    override var `$vocabulary`: MutableMap<UriString, Boolean>? = null,
    override var `$comment`: String? = null,
    override var `$defs`: MutableMap<String, MutableJsonSchema>? = null,

    // meta/applicator
    override var prefixItems: MutableList<MutableJsonSchema>? = null,
    override var items: MutableJsonSchema? = null,
    override var contains: MutableJsonSchema? = null,
    override var additionalProperties: MutableJsonSchema? = null,
    override var properties: MutableMap<String, MutableJsonSchema>? = null,
    override var patternProperties: MutableMap<Regex, MutableJsonSchema>? = null,
    override var dependentSchemas: MutableMap<String, MutableJsonSchema>? = null,
    override var propertyNames: MutableJsonSchema? = null,
    override var `if`: MutableJsonSchema? = null,
    override var then: MutableJsonSchema? = null,
    override var `else`: MutableJsonSchema? = null,
    override var allOf: MutableList<MutableJsonSchema>? = null,
    override var anyOf: MutableList<MutableJsonSchema>? = null,
    override var oneOf: MutableList<MutableJsonSchema>? = null,
    override var not: MutableJsonSchema? = null,

    // meta/unevaluated
    override var unevaluatedItems: MutableJsonSchema? = null,
    override var unevaluatedProperties: MutableJsonSchema? = null,

    // meta/validation
    override var type: Union<SimpleType, MutableList<SimpleType>>? = null,
    override var enum: MutableList<*>? = null,
    override var multipleOf: Int? = null,
    override var maximum: BigDecimal? = null,
    override var exclusiveMaximum: BigDecimal? = null,
    override var minimum: BigDecimal? = null,
    override var exclusiveMinimum: BigDecimal? = null,
    override var maxLength: Int? = null,
    override var minLength: Int? = null,
    override var pattern: Regex? = null,
    override var maxItems: Int? = null,
    override var minItems: Int? = null,
    override var uniqueItems: Boolean? = null,
    override var maxContains: Int? = null,
    override var minContains: Int? = null,
    override var maxProperties: Int? = null,
    override var minProperties: Int? = null,
    override var required: MutableList<String>? = null,
    override var dependentRequired: MutableMap<String, MutableList<String>>? = null,

    // meta/meta-data
    override var title: String? = null,
    override var description: String? = null,
    override var deprecated: Boolean? = null,
    override var readOnly: Boolean? = null,
    override var writeOnly: Boolean? = null,
    override var examples: MutableList<String>? = null,
    override var default: Any? = null,

    // meta/format-annotation
    override var format: String? = null,

    // meta/content
    override var contentEncoding: String? = null,
    override var contentMediaType: String? = null,
    override var contentSchema: MutableJsonSchema? = null,

    // emf-jsonschema/java-type
    override var javaType: String? = null,

    // emf-jsonschema/package
    override var namespace: String? = null,

    // emf-jsonschema/class
    override var abstract: Boolean? = null,
    override var `interface`: Boolean? = null,

    // emf-jsonschema/enum
    override var enumNames: List<String>? = null,
    override var enumIndices: List<Int>? = null,

    // emf-jsonschema/structural-features
    override var volatile: Boolean? = null,
    override var derived: Boolean? = null,
    override var unordered: Boolean? = null,
    override var transientProperties: MutableMap<String, JsonSchema>? = null,
    override var reference: Boolean? = null,

    // emf-jsonschema/attributes
    override var id: Boolean? = null,

    // emf-jsonschema/references
    override var containment: Boolean? = null,
    override var resolveProxies: Boolean? = null,
    override var opposite: String? = null,

    // emf-jsonschema/methods
    override var methods: MutableMap<String, MutableJsonSchemaMethod>? = null,

    // emf-jsonschema/method/$defs/argument
    override var argumentName: String? = null,
) : MutableJsonSchemaParameter
