package io.freund.adrian.emfjsonschema.utils

import io.kotest.assertions.fail
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class TestUnion :
    FunSpec({
        class A

        class B

        val a = A()
        val b = B()

        test("left union member") {
            val union: Union<A, B> = Union.Left(a)

            when (union) {
                is Union.Left -> union.value shouldBe a
                is Union.Right -> fail("Should be unreachable")
            }
        }

        test("right union member") {
            val union: Union<A, B> = Union.Right(b)

            when (union) {
                is Union.Left -> fail("Should be unreachable")
                is Union.Right -> union.value shouldBe b
            }
        }

        test("left union member extension function") {
            val union: Union<A, B> = a.left()

            when (union) {
                is Union.Left -> union.value shouldBe a
                is Union.Right -> fail("Should be unreachable")
            }
        }

        test("right union member extension function") {
            val union: Union<A, B> = b.right()

            when (union) {
                is Union.Left -> fail("Should be unreachable")
                is Union.Right -> union.value shouldBe b
            }
        }
    })
