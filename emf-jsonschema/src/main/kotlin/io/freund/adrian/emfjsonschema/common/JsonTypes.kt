package io.freund.adrian.emfjsonschema.common

import io.freund.adrian.emfjsonschema.schema.JsonSchema
import io.freund.adrian.emfjsonschema.schema.SimpleType
import io.freund.adrian.emfjsonschema.utils.toBigDecimal
import java.math.BigInteger
import java.util.Date
import kotlin.reflect.KClass

fun schemaToType(simpleType: SimpleType, schema: JsonSchema, nullable: Boolean): KClass<*> {
    when (simpleType) {
        SimpleType.STRING -> {
            if (schema.format == "date-time") {
                return Date::class
            }
            val min = schema.minLength
            val max = schema.maxLength
            return if (min == 1 && max == 1) {
                if (nullable) {
                    java.lang.Character::class
                } else {
                    Char::class
                }
            } else {
                String::class
            }
        }
        SimpleType.BOOLEAN -> {
            return if (nullable) {
                java.lang.Boolean::class
            } else {
                Boolean::class
            }
        }
        SimpleType.INTEGER -> {
            val min = schema.minimum
            val max = schema.maximum
            return if (min == null || max == null) {
                // Default to Long. Probably not worth the performance penalty to default to BigInteger
                if (nullable) {
                    java.lang.Long::class
                } else {
                    Long::class
                }
            } else if (min >= Short.MIN_VALUE.toBigDecimal() && max <= Short.MAX_VALUE.toBigDecimal()) {
                if (nullable) {
                    java.lang.Short::class
                } else {
                    Short::class
                }
            } else if (min >= Int.MIN_VALUE.toBigDecimal() && max <= Int.MAX_VALUE.toBigDecimal()) {
                if (nullable) {
                    java.lang.Integer::class
                } else {
                    Int::class
                }
            } else if (min >= Long.MIN_VALUE.toBigDecimal() && max <= Long.MAX_VALUE.toBigDecimal()) {
                if (nullable) {
                    java.lang.Long::class
                } else {
                    Long::class
                }
            } else {
                BigInteger::class
            }
        }
        SimpleType.NULL -> {
            return Void::class
        }
        SimpleType.NUMBER -> {
            // Default to Double. Probably not worth the performance penalty to default to BigDecimal
            return if (nullable) {
                java.lang.Double::class
            } else {
                Double::class
            }
        }
        SimpleType.OBJECT, SimpleType.ARRAY -> error("object and array types should be handled on the structural feature level")
    }
}
