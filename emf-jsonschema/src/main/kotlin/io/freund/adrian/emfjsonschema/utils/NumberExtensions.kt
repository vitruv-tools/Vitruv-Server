package io.freund.adrian.emfjsonschema.utils

import java.math.BigDecimal
import java.math.BigInteger

fun Number.toBigInteger() = when (this) {
    is BigInteger -> this
    is BigDecimal -> this.toBigInteger() // Not a recursive call. This is a member function for BigDecimal
    else -> BigInteger.valueOf(this.toLong())
}
fun Number.toBigDecimal() = when (this) {
    is BigDecimal -> this
    is BigInteger -> BigDecimal(this)
    else -> BigDecimal.valueOf(this.toDouble())
}
