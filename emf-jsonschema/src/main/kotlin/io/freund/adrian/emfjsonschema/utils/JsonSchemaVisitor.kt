package io.freund.adrian.emfjsonschema.utils

import io.freund.adrian.emfjsonschema.schema.JsonSchema

abstract class JsonSchemaVisitor<T> {
    abstract fun visitSchemata(schemata: List<JsonSchema>): List<T>
}
