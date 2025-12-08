package io.freund.adrian.emfjsonschema.utils

import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.BeanProperty
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.ContextualDeserializer
import com.fasterxml.jackson.databind.exc.MismatchedInputException

/**
 * A JSON serializable union type
 */
@JsonDeserialize(using = UnionDeserializer::class)
sealed class Union<out A, out B> {
    data class Left<out A>(
        @JsonValue val value: A,
    ) : Union<A, Nothing>()

    data class Right<out B>(
        @JsonValue val value: B,
    ) : Union<Nothing, B>()
}

fun <A> A.left() = Union.Left(this)

fun <B> B.right() = Union.Right(this)

class UnionDeserializer(val leftType: JavaType?, val rightType: JavaType?) :
    JsonDeserializer<Union<*, *>>(),
    ContextualDeserializer {

    // This constructor looks unused, but is required by jackson.
    @Suppress("UNUSED")
    constructor() : this(null, null)

    override fun createContextual(ctxt: DeserializationContext, property: BeanProperty): JsonDeserializer<*> {
        val leftType = property.type.containedType(0)
        val rightType = property.type.containedType(1)
        return UnionDeserializer(leftType, rightType)
    }

    override fun deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext): Union<*, *> {
        // There is no way to know which side of the union the type should deserialize into, so we just
        // try the left side first
        try {
            val left: Any = deserializationContext.readValue(jsonParser, leftType)
            return left.left()
        } catch (e: MismatchedInputException) {
            try {
                val right: Any = deserializationContext.readValue(jsonParser, rightType)
                return right.right()
            } catch (e2: MismatchedInputException) {
                throw MismatchedInputException.from(jsonParser, deserializationContext.contextualType, "Failed to deserialize into Union")
            }
        }
    }
}
