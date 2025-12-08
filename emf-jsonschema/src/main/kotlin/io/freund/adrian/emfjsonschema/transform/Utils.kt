package io.freund.adrian.emfjsonschema.transform

import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EClassifier
import org.eclipse.emf.ecore.EReference
import org.eclipse.emf.ecore.EcoreFactory
import org.eclipse.emf.ecore.InternalEObject
import java.net.URI
import kotlin.io.path.Path

// We need to differentiate between java and emf URIs
typealias EmfUri = org.eclipse.emf.common.util.URI

val URI_MAP = mapOf(
    // We use our own standard library to represent Ecore types
    "http://www.eclipse.org/emf/2002/Ecore" to "https://emf-jsonschema.adrian.freund.io/draft/1/types",
)
val REVERSE_URI_MAP = URI_MAP.map { (key, value) -> value to key }.toMap()

fun classifierToSchemaId(classifier: EClassifier): String {
    if (classifier.eIsProxy()) {
        // The referenced model couldn't be loaded, so we instead translate the URI.
        val uri = (classifier as InternalEObject).eProxyURI()
        return ecoreUriToSchemaId(uri)
    }
    val nsUri = classifier.ePackage.nsURI

    return "${URI_MAP.getOrDefault(nsUri, nsUri)}/${classifier.name}.json"
}

fun ecoreUriToSchemaId(uri: EmfUri): String {
    val scheme = uri.scheme() ?: ""
    val host = uri.host() ?: ""
    val path = uri.path() ?: ""
    val base = "$scheme://$host$path"
    var fragment = uri.fragment() ?: ""
    if (!fragment.startsWith("//")) {
        error { "Only top-level ecore objects can be referenced" }
    } else {
        fragment = fragment.removePrefix("//")
    }
    return "${URI_MAP.getOrDefault(base, base)}/$fragment.json"
}

fun schemaIdToEcoreUri(id: URI): EmfUri {
    val scheme = id.scheme ?: ""
    val host = id.host ?: ""
    val path = Path(id.path)
    val element = path.fileName.toString().removeSuffix(".json")
    val directory = path.parent.toString()

    val base = "$scheme://$host$directory"
    val emfUri = "${REVERSE_URI_MAP.getOrDefault(base, base)}#//$element"

    return EmfUri.createURI(emfUri)
}

fun schemaIdToClassifier(schemaId: String, registry: Map<String, EClassifier>): EClassifier {
    var classifier = registry[schemaId]
    if (classifier == null) {
        val uri = schemaIdToEcoreUri(URI(schemaId))
        classifier = EcoreFactory.eINSTANCE.createEDataType()
        (classifier as InternalEObject).eSetProxyURI(uri)
    }
    return classifier
}

fun referenceToSchemaId(reference: EReference): String = classifierToSchemaId(reference.eOpposite.eType) + "#/${reference.name}"

fun schemaIdToReference(schemaId: String, registry: Map<String, EClassifier>): EReference {
    val split = schemaId.split("#")
    val schema = split.first()
    val fragment = split.last().removePrefix("/")
    val type = schemaIdToClassifier(schema, registry)
    require(type is EClass)
    if (!type.eIsProxy()) {
        val reference = type.eAllReferences.filter { it.name == fragment }
        if (reference.isEmpty()) {
            error("Trying to use nonexistent reference '$fragment' as opposite reference")
        }
        return reference.first()
    } else {
        val reference = EcoreFactory.eINSTANCE.createEReference()
        reference.eType = type
        reference.name = fragment

        return reference
    }
}
