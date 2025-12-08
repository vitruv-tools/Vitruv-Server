package io.freund.adrian.emfjsonschema.schema

import com.networknt.schema.AnnotationKeyword
import com.networknt.schema.DisallowUnknownKeywordFactory
import com.networknt.schema.JsonMetaSchema
import com.networknt.schema.JsonSchema
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SchemaLocation
import com.networknt.schema.SchemaValidatorsConfig
import com.networknt.schema.SpecVersion
import com.networknt.schema.Vocabulary
import com.networknt.schema.VocabularyFactory
import io.freund.adrian.emfjsonschema.EMF_JSONSCHEMA_ID_PREFIX

/**
 * A class to creating a JSON-Schema validator enriched with support for the extended EMF-JsonSchema.
 */
object EmfJsonSchemaFactory {
    fun getMetaSchema(): JsonMetaSchema {
        val vocabularyFactory = VocabularyFactory {
            when (it) {
                "${EMF_JSONSCHEMA_ID_PREFIX}/vocabs/java-type" -> Vocabulary(it, AnnotationKeyword("javaType"))
                "${EMF_JSONSCHEMA_ID_PREFIX}/vocabs/package" -> Vocabulary(it, AnnotationKeyword("namespace"))
                "${EMF_JSONSCHEMA_ID_PREFIX}/vocabs/attribute" -> Vocabulary(it, AnnotationKeyword("id"))
                "${EMF_JSONSCHEMA_ID_PREFIX}/vocabs/class" -> Vocabulary(
                    it,
                    AnnotationKeyword("abstract"),
                    AnnotationKeyword("interface"),
                )
                "${EMF_JSONSCHEMA_ID_PREFIX}/vocabs/enum" -> Vocabulary(it, AnnotationKeyword("enumNames"))
                "${EMF_JSONSCHEMA_ID_PREFIX}/vocabs/method" -> Vocabulary(it, AnnotationKeyword("methods"))
                "${EMF_JSONSCHEMA_ID_PREFIX}/vocabs/reference" -> Vocabulary(
                    it,
                    AnnotationKeyword("containment"),
                    AnnotationKeyword("resolveProxies"),
                    AnnotationKeyword("opposite"),
                )
                "${EMF_JSONSCHEMA_ID_PREFIX}/vocabs/structural-feature" -> Vocabulary(
                    it,
                    AnnotationKeyword("volatile"),
                    AnnotationKeyword("derived"),
                    AnnotationKeyword("unordered"),
                    AnnotationKeyword("transientProperties"),
                    AnnotationKeyword("reference"),
                )
                else -> null
            }
        }

        val metaSchemaBuilder = JsonMetaSchema.builder(JsonMetaSchema.getV202012())
            .vocabularyFactory(vocabularyFactory)
            .unknownKeywordFactory(DisallowUnknownKeywordFactory.getInstance())

        return metaSchemaBuilder.build()
    }
    fun getFactory(): JsonSchemaFactory {
        val schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012) {
            it.metaSchema(this.getMetaSchema())
            it.schemaMappers { mapper ->
                mapper.mapPrefix(EMF_JSONSCHEMA_ID_PREFIX, "classpath:schema")
            }
        }
        return schemaFactory
    }

    fun getEmfJsonSchema(config: SchemaValidatorsConfig? = null): JsonSchema {
        val schema = this.getFactory().getSchema(
            SchemaLocation.of("${EMF_JSONSCHEMA_ID_PREFIX}/schema.json"),
            config ?: SchemaValidatorsConfig.builder().build(),
        )

        return schema
    }
}
