package io.freund.adrian.emfjsonschema

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.eclipse.emf.common.util.URI
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.resource.ResourceSet
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl
import org.eclipse.emfcloud.jackson.resource.JsonResourceFactory

class TestBasic :
    FunSpec({
        test("loading an Ecore model works") {
            val rs: ResourceSet = ResourceSetImpl()
            rs.resourceFactoryRegistry.extensionToFactoryMap["ecore"] = EcoreResourceFactoryImpl()

            val resource =
                rs.getResource(
                    URI.createFileURI(javaClass.getResource("/Coffee.ecore")?.path),
                    true,
                )
            val ePackage = resource.contents[0] as EPackage

            ePackage.nsURI shouldBe "http://www.eclipse.org/emfcloud/coffee/model"
        }

        test("loading an instance works") {
            val rs: ResourceSet = ResourceSetImpl()
            rs.resourceFactoryRegistry.extensionToFactoryMap["ecore"] = EcoreResourceFactoryImpl()
            rs.resourceFactoryRegistry.extensionToFactoryMap["xmi"] = XMIResourceFactoryImpl()
            rs.resourceFactoryRegistry.extensionToFactoryMap["json"] = JsonResourceFactory()

            rs.getResource(
                URI.createFileURI(javaClass.getResource("/Coffee.ecore")?.path),
                true,
            )

            val resource2 =
                rs.getResource(
                    URI.createFileURI(javaClass.getResource("/SuperBrewer3000.xmi")?.path),
                    true,
                )
            val superBrewer3000 = resource2.contents[0]

            val resourceOut = rs.createResource(URI.createFileURI("SuperBrewer3000.json"))
            resourceOut.contents.add(superBrewer3000)
            resourceOut.save(null)
        }
    })
