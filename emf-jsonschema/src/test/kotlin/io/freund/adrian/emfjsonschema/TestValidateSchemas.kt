package io.freund.adrian.emfjsonschema

import com.networknt.schema.InputFormat
import io.freund.adrian.emfjsonschema.schema.EmfJsonSchemaFactory
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.collections.shouldBeEmpty

class TestValidateSchemas :
    FunSpec({
        context("Validate meta-schema extensions against meta-schema") {
            val schema = EmfJsonSchemaFactory.getEmfJsonSchema()

            withData(
                "types/EBigDecimal.json",
                "types/EBigInteger.json",
                "types/EBoolean.json",
                "types/EBooleanObject.json",
                "types/EChar.json",
                "types/ECharacterObject.json",
                "types/EDouble.json",
                "types/EDoubleObject.json",
                "types/EFloat.json",
                "types/EFloatObject.json",
                "types/EInt.json",
                "types/EIntegerObject.json",
                "types/ELong.json",
                "types/ELongObject.json",
                "types/EMap.json",
                "types/EShort.json",
                "types/EShortObject.json",
                "types/EString.json",
                "method-basic.json",
                "method-no-parameters.json",
            ) { file: String ->
                val input = javaClass.getResource("/schema/$file")!!.readText()

                val validationMessages = schema.validate(input, InputFormat.JSON)

                validationMessages.shouldBeEmpty()
            }
        }
    })
