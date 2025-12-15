package tools.vitruv.framework.remote;

import java.nio.file.Path;
import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.impl.EClassImpl;
import org.eclipse.emf.ecore.impl.EObjectImpl;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.annotation.JsonAppend.Attr;

import edu.kit.ipd.sdq.metamodels.families.FamiliesFactory;
import edu.kit.ipd.sdq.metamodels.families.FamiliesPackage;
import edu.kit.ipd.sdq.metamodels.families.Family;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.eobject.CreateEObject;
import tools.vitruv.change.atomic.eobject.DeleteEObject;
import tools.vitruv.change.atomic.eobject.EObjectExistenceEChange;
import tools.vitruv.change.atomic.eobject.EobjectFactory;
import tools.vitruv.change.atomic.eobject.impl.CreateEObjectImpl;
import tools.vitruv.change.atomic.feature.FeatureFactory;
import tools.vitruv.change.atomic.feature.attribute.AttributeFactory;
import tools.vitruv.change.atomic.feature.attribute.ReplaceSingleValuedEAttribute;
import tools.vitruv.change.atomic.root.InsertRootEObject;
import tools.vitruv.change.atomic.root.RootFactory;
import tools.vitruv.change.composite.description.VitruviusChange;
import tools.vitruv.change.composite.description.impl.TransactionalChangeImpl;
import tools.vitruv.change.testutils.TestUserInteraction;
import tools.vitruv.framework.remote.common.json.JsonMapper;
import tools.vitruv.framework.vsum.VirtualModelBuilder;


/**
 * Test methods for the serialization of {@link EChange}s.
 */
public class SerializationTest {
    private JsonMapper mapper;

    private final FamiliesFactory familiesFactory = FamiliesFactory.eINSTANCE;
    private final EobjectFactory existenceChangeFactory = EobjectFactory.eINSTANCE;
    private final RootFactory rootChangeFactory = RootFactory.eINSTANCE;
    private final AttributeFactory attributeChangeFactory = AttributeFactory.eINSTANCE;

    @Test
    void testSerializationOfEmptyEChange(@TempDir Path testdir) throws JsonProcessingException {
        var vsum = new VirtualModelBuilder()
            .withStorageFolder(testdir)
            .withUserInteractorForResultProvider(new TestUserInteraction.ResultProvider(new TestUserInteraction()))
            .buildAndInitialize();

        mapper = new JsonMapper(testdir);

        var resource = new ResourceImpl();
        resource.setURI(URI.createFileURI(testdir.toString() + "/example.family"));
        var family = familiesFactory.createFamily();
        family.setLastName("Modeler");

        CreateEObject<EObject> createElementChange = existenceChangeFactory.createCreateEObject();
        createElementChange.setAffectedElement(family);
        createElementChange.setAffectedEObjectType(family.eClass());

        ReplaceSingleValuedEAttribute<EObject, String> changeFamilyName = attributeChangeFactory.createReplaceSingleValuedEAttribute();
        changeFamilyName.setAffectedElement(family);
        changeFamilyName.setAffectedFeature(FamiliesPackage.eINSTANCE.getFamily_LastName());
        changeFamilyName.setOldValue("Modeler");
        changeFamilyName.setNewValue("Developer");

        InsertRootEObject<EObject> insertFamilyChange = rootChangeFactory.createInsertRootEObject();
        insertFamilyChange.setIndex(0);
        insertFamilyChange.setNewValue(family);
        insertFamilyChange.setResource(resource);
        insertFamilyChange.setUri(resource.getURI().toFileString());

        DeleteEObject<EObject> deleteElementChange = existenceChangeFactory.createDeleteEObject();
        deleteElementChange.setAffectedElement(family);
        deleteElementChange.setAffectedEObjectType(family.eClass());

        VitruviusChange<EObject> change = new TransactionalChangeImpl<>(
            List.of(createElementChange, changeFamilyName, insertFamilyChange, deleteElementChange)
        );
        final String serialization = mapper.serialize(change);
        System.out.println(serialization);
    }
}
