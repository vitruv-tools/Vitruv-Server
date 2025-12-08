package io.freund.adrian.emfjsonschema.schema

import kotlin.reflect.full.memberProperties

fun JsonSchema.isPureReference(): Boolean {
    JsonSchema::class.memberProperties.forEach {
        if (it.name != "\$ref") {
            if (it.get(this) != null) return false
        }
    }
    return this.`$ref` != null
}

fun JsonSchema.isEmpty(): Boolean {
    JsonSchema::class.memberProperties.forEach {
        if (it.get(this) != null) return false
    }
    return true
}

fun JsonSchema.isSimpleType(): Boolean {
    JsonSchema::class.memberProperties.forEach {
        if (it.name != "type") {
            if (it.get(this) != null) return false
        }
    }
    return this.type != null
}
