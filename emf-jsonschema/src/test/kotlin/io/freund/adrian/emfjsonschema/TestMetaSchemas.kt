package io.freund.adrian.emfjsonschema

import com.networknt.schema.InputFormat
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SchemaId
import com.networknt.schema.SchemaLocation
import com.networknt.schema.SchemaValidatorsConfig
import com.networknt.schema.SpecVersion
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.collections.shouldBeEmpty

class TestMetaSchemas :
    FunSpec({
        context("Validate meta-schema extensions against meta-schema") {
            val schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)
            val builder = SchemaValidatorsConfig.builder()
            val config = builder.build()
            val schema = schemaFactory.getSchema(SchemaLocation.of(SchemaId.V202012), config)

            withData(
                "class.json",
                "structural-feature.json",
                "attribute.json",
                "reference.json",
                "method.json",
                "schema.json",
            ) { file ->
                val input = javaClass.getResource("/schema/$file")!!.readText()

                val validationMessages = schema.validate(input, InputFormat.JSON)

                validationMessages.shouldBeEmpty()
            }
        }
    })
