package io.freund.adrian.emfjsonschema

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import io.freund.adrian.emfjsonschema.schema.JsonSchemaImpl
import io.freund.adrian.emfjsonschema.schema.SimpleType
import io.freund.adrian.emfjsonschema.transform.EcoreTransformer
import io.freund.adrian.emfjsonschema.transform.EmfUri
import io.freund.adrian.emfjsonschema.transform.JsonSchemaTransformer
import io.freund.adrian.emfjsonschema.transform.ecoreUriToSchemaId
import io.freund.adrian.emfjsonschema.transform.schemaIdToEcoreUri
import io.freund.adrian.emfjsonschema.utils.Union
import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.engine.spec.tempfile
import io.kotest.matchers.shouldBe
import org.eclipse.emf.common.util.URI
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.EcoreFactory
import org.eclipse.emf.ecore.resource.ResourceSet
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl
import java.io.ByteArrayOutputStream
import java.util.Date
import kotlin.collections.set

@OptIn(ExperimentalKotest::class)
class TestTransformation :
    FunSpec({
        context("transform URIs") {
            withData(
                nameFn = { "${it.first} to ${it.second}" },
                "http://www.eclipse.org/emf/2002/Ecore#//EInt" to "https://emf-jsonschema.adrian.freund.io/draft/1/types/EInt.json",
                "http://www.eclipse.org/emfcloud/coffee/model#//Workflow" to "http://www.eclipse.org/emfcloud/coffee/model/Workflow.json",
            ) { (emfUri, schemaId) ->
                ecoreUriToSchemaId(EmfUri.createURI(emfUri)) shouldBe schemaId
                schemaIdToEcoreUri(java.net.URI(schemaId)).toString() shouldBe emfUri
            }
        }
        context("transform EDataType") {
            val eDataType = EcoreFactory.eINSTANCE.createEDataType()

            withData(
                nameFn = { "${it.first} to ${it.second}" },
                Boolean::class.java to Union.Left(SimpleType.BOOLEAN),
                java.lang.Boolean::class.java to Union.Right(listOf(SimpleType.BOOLEAN, SimpleType.NULL)),
                Short::class.java to Union.Left(SimpleType.INTEGER),
                java.lang.Short::class.java to Union.Right(listOf(SimpleType.INTEGER, SimpleType.NULL)),
                Int::class.java to Union.Left(SimpleType.INTEGER),
                java.lang.Integer::class.java to Union.Right(listOf(SimpleType.INTEGER, SimpleType.NULL)),
                Long::class.java to Union.Left(SimpleType.INTEGER),
                java.lang.Long::class.java to Union.Right(listOf(SimpleType.INTEGER, SimpleType.NULL)),
                // Float::class.java to Union.Left(SimpleType.NUMBER),
                Double::class.java to Union.Left(SimpleType.NUMBER),
                Char::class.java to Union.Left(SimpleType.STRING),
                java.lang.Character::class.java to Union.Right(listOf(SimpleType.STRING, SimpleType.NULL)),
                String::class.java to Union.Right(listOf(SimpleType.STRING, SimpleType.NULL)),
                Date::class.java to Union.Right(listOf(SimpleType.STRING, SimpleType.NULL)),
            ) { pair ->
                eDataType.instanceClass = pair.first
                val schema = EcoreTransformer().visitDataType(eDataType)

                schema.type shouldBe pair.second

                val eDataType2 = JsonSchemaTransformer().visitDataTypeSchema(schema)

                eDataType2.instanceClass shouldBe pair.first
            }
        }

        test("transform EEnum") {
            val eEnum = EcoreFactory.eINSTANCE.createEEnum()
            val literal0 = EcoreFactory.eINSTANCE.createEEnumLiteral().also {
                it.name = "A"
                it.value = 0
            }
            val literal1 = EcoreFactory.eINSTANCE.createEEnumLiteral().also {
                it.name = "B"
                it.value = 1
            }
            val literal2 = EcoreFactory.eINSTANCE.createEEnumLiteral().also {
                it.name = "C"
                it.value = 2
            }
            eEnum.eLiterals.add(literal0)
            eEnum.eLiterals.add(literal1)
            eEnum.eLiterals.add(literal2)

            val schema = EcoreTransformer().visitEnum(eEnum)

            schema.enum shouldBe listOf("A", "B", "C")

            val eEnum2 = JsonSchemaTransformer().visitEnumSchema(schema)

            eEnum2.getEEnumLiteral(0).name shouldBe "A"
            eEnum2.getEEnumLiteral(1).name shouldBe "B"
            eEnum2.getEEnumLiteral(2).name shouldBe "C"
        }

        test("transform Package") {
            val ePackage = EcoreFactory.eINSTANCE.createEPackage()
        }

        context("file based transformations").config(enabled = false) {
            val ecoreTransformer = EcoreTransformer()
            val jsonSchemaTransformer = JsonSchemaTransformer()
            val rs: ResourceSet = ResourceSetImpl()
            rs.resourceFactoryRegistry.extensionToFactoryMap["ecore"] = EcoreResourceFactoryImpl()
            rs.resourceFactoryRegistry.protocolToFactoryMap["classpath"] = EcoreResourceFactoryImpl()

            val objectMapper = ObjectMapper()
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT)

            withData(
                "package.ecore" to "package.json",
                "Coffee.ecore" to "Coffee.json",
            ) { (eCoreFile, jsonFile) ->
                val resource = rs.getResource(URI.createURI(javaClass.getResource("/transform/$eCoreFile")!!.path), true)
                val ePackage = resource.contents[0] as EPackage

                val schemata = ecoreTransformer.visitPackage(ePackage)

                val jsonString = objectMapper.writeValueAsString(schemata)

                schemata shouldBe objectMapper.readValue(javaClass.getResource("/transform/$jsonFile")!!.readText(), object : TypeReference<Map<String, JsonSchemaImpl>>() {})

                val ePackage2 = jsonSchemaTransformer.visitSchemata(schemata.values.toList())
                val resource2 = rs.createResource(URI.createFileURI(tempfile(suffix = ".ecore").path))
                resource2.contents.addAll(jsonSchemaTransformer.packages.values)

                val outputStream = ByteArrayOutputStream()
                resource2.save(outputStream, emptyMap<Nothing, Nothing>())

                val result = String(outputStream.toByteArray())

                result shouldBe javaClass.getResource("/transform/$eCoreFile")!!.readText()
            }
        }
    })
