package io.freund.adrian.emfjsonschema

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.defaultLazy
import com.github.ajalt.clikt.parameters.arguments.help
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.path
import com.networknt.schema.InputFormat
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SchemaLocation
import com.networknt.schema.SchemaValidatorsConfig
import com.networknt.schema.SpecVersion
import io.freund.adrian.emfjsonschema.generate.CodeGenerationVisitor
import io.freund.adrian.emfjsonschema.schema.EmfJsonSchemaFactory
import io.freund.adrian.emfjsonschema.schema.JsonSchemaImpl
import io.freund.adrian.emfjsonschema.transform.EcoreTransformer
import io.freund.adrian.emfjsonschema.transform.JsonSchemaTransformer
import io.freund.adrian.emfjsonschema.utils.getFilesRecursive
import io.freund.adrian.emfjsonschema.utils.toEmfUri
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.resource.ResourceSet
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl
import org.eclipse.emfcloud.jackson.resource.JsonResourceFactory
import java.nio.charset.Charset
import kotlin.io.path.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.name

class EmfJsonSchemaCli : CliktCommand() {
    init {
        subcommands(Convert(), Validate(), Generate(), Encode())
    }

    override fun run() = Unit
}

class Convert : CliktCommand() {
    private val input by argument().path(mustExist = true).help("The input file or directory")
    private val output by argument().path().help("The output file or directory").defaultLazy {
        Path(".")
    }

    override fun run() {
        val rs: ResourceSet = ResourceSetImpl()
        rs.resourceFactoryRegistry.extensionToFactoryMap["ecore"] = EcoreResourceFactoryImpl()

        val objectMapper = ObjectMapper()
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT)

        if (!input.isDirectory()) {
            val resource = rs.getResource(input.toEmfUri(), true)
            val ePackage = resource.contents[0] as EPackage

            val schemata = EcoreTransformer().visitPackage(ePackage)

            if (!output.exists()) {
                output.createDirectory()
            }

            for (entry in schemata) {
                val outFile = (output / entry.key).toFile()
                outFile.parentFile.mkdirs()
                objectMapper.writeValue(outFile, entry.value)
            }
        } else {
            val files = getFilesRecursive(listOf(input))
            val schemata = files.map {
                objectMapper.readValue(it.toFile(), JsonSchemaImpl::class.java)
            }.toList()

            val models = JsonSchemaTransformer().visitSchemata(schemata)

            val resource = rs.createResource(output.toEmfUri())
            resource.contents.addAll(models)
            resource.save(emptyMap<Nothing, Nothing>())
        }
    }
}

class Validate : CliktCommand() {
    private val mainSchema by argument().path(mustExist = true).help("The main schema")
    private val schemata by argument().path(mustExist = true).multiple().help("Supplementary schema files")
    private val input by argument().file(mustExist = true).help("The input file")

    override fun run() {
        println(schemata)
        val mapper = ObjectMapper()
        val schemaIds = mutableMapOf<String, String>()
        val files = getFilesRecursive(schemata)
        files.forEach { file ->
            val node = mapper.readTree(file.toFile())
            val schemaId = node.at("/\$id").textValue()
            schemaIds[schemaId] = file.toUri().toString()
        }
        val mainNode = mapper.readTree(mainSchema.toFile())
        val schemaId = mainNode.at("/\$id").textValue()

        val factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012) {
            it.metaSchema(EmfJsonSchemaFactory.getMetaSchema())
            it.schemaMappers { mapper ->
                mapper.mapPrefix(EMF_JSONSCHEMA_ID_PREFIX, "classpath:schema")
                mapper.mappings(schemaIds)
            }
        }
        val validator = factory.getSchema(
            SchemaLocation.of(schemaId),
            SchemaValidatorsConfig.builder().build(),
        )
        val validationMessages =
            validator.validate(
                input.readText(Charset.defaultCharset()),
                InputFormat.JSON,
            )

        echo(validationMessages.size)
        for (validationMessage in validationMessages) {
            echo(validationMessage.message)
        }
    }
}

class Generate : CliktCommand() {
    private val input by argument().path(mustExist = true).multiple().help("The input JSON schema")
    private val output by argument().path().help("The output directory")

    override fun run() {
        val files = getFilesRecursive(input)

        val objectMapper = ObjectMapper()
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT)

        val schemata = files.associateBy(
            {
                it.name.split('.').first()
            },
            {
                objectMapper.readValue(it.toFile(), JsonSchemaImpl::class.java)
            },
        )
        val generator = CodeGenerationVisitor()
        val fileSpecs = generator.visitSchemata(schemata)
        fileSpecs.forEach {
            it.writeTo(output)
        }
    }
}

class Encode : CliktCommand() {
    private val metaModel by argument().file(mustExist = true).help("The ecore meta-model")
    private val instance by argument().file(mustExist = true).help("The ecore instance")
    private val output by argument().file().help("The json output file")

    override fun run() {
        val rs: ResourceSet = ResourceSetImpl()
        rs.resourceFactoryRegistry.extensionToFactoryMap["ecore"] = EcoreResourceFactoryImpl()
        val metaModelResource = rs.getResource(metaModel.toEmfUri(), true)
        val ePackage = metaModelResource.contents[0] as EPackage

        rs.resourceFactoryRegistry.extensionToFactoryMap[instance.extension] = XMIResourceFactoryImpl()
        rs.resourceFactoryRegistry.extensionToFactoryMap["json"] = JsonResourceFactory()
        val instanceResource = rs.getResource(instance.toEmfUri(), true)
        println(instanceResource)
        val outputResource = rs.createResource(output.toEmfUri())
        outputResource.contents.addAll(instanceResource.contents)
        outputResource.save(null)
    }
}

fun main(args: Array<String>) = EmfJsonSchemaCli().main(args)
