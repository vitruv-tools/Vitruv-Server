package tools.vitruv.framework.remote.modules.vsums.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import tools.vitruv.framework.remote.helper.IntegrationTest;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@IntegrationTest
class MetamodelControllerTest {
    @Autowired
    private MockMvc mvc;

    @Test
    public void testMetamodelNames() throws Exception {
        mvc.perform(get("/v1/metamodels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasItem("SystemRootVsum")))
                .andExpect(jsonPath("$", hasItem("InteractionDemoVsum")));
    }

    @Test
    public void testLoadEcoreModels() throws Exception {
        mvc.perform(get("/v1/metamodels/SystemRootVsum/ecore-models"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(content().json("[" + ECORE_MODEL + "," + ECORE_MODEL2 + "]", false));
    }

    private static final String ECORE_MODEL = "{\"version\":\"2.0\",\"name\":\"model\",\"nsURI\":\"http://vitruv.tools/methodologisttemplate/model\",\"nsPrefix\":\"model\",\"eClassifiers\":[{\"type\":\"ecore:EClass\",\"name\":\"Component\",\"eStructuralFeatures\":[{\"type\":\"ecore:EAttribute\",\"name\":\"name\",\"lowerBound\":\"1\",\"eType\":\"ecore:EDataType http://www.eclipse.org/emf/2002/Ecore#//EString\"},{\"type\":\"ecore:EReference\",\"name\":\"supportedProtocols\",\"lowerBound\":\"1\",\"upperBound\":\"-1\",\"eType\":\"#//Protocol\"}]},{\"type\":\"ecore:EClass\",\"name\":\"Link\",\"eStructuralFeatures\":[{\"type\":\"ecore:EAttribute\",\"name\":\"speedMBitsPerSecond\",\"lowerBound\":\"1\",\"eType\":\"ecore:EDataType http://www.eclipse.org/emf/2002/Ecore#//EInt\"},{\"type\":\"ecore:EReference\",\"name\":\"components\",\"lowerBound\":\"2\",\"upperBound\":\"-1\",\"eType\":\"#//Component\"},{\"type\":\"ecore:EReference\",\"name\":\"protocol\",\"lowerBound\":\"1\",\"eType\":\"#//Protocol\"},{\"type\":\"ecore:EReference\",\"name\":\"supportedProtocols\",\"lowerBound\":\"1\",\"upperBound\":\"-1\",\"eType\":\"#//Protocol\"}]},{\"type\":\"ecore:EClass\",\"name\":\"Protocol\",\"eStructuralFeatures\":{\"type\":\"ecore:EAttribute\",\"name\":\"name\",\"lowerBound\":\"1\",\"eType\":\"ecore:EDataType http://www.eclipse.org/emf/2002/Ecore#//EString\"}},{\"type\":\"ecore:EClass\",\"name\":\"Device\",\"eSuperTypes\":\"#//Component\"},{\"type\":\"ecore:EClass\",\"name\":\"Server\",\"eSuperTypes\":\"#//Component\"},{\"type\":\"ecore:EClass\",\"name\":\"System\",\"eStructuralFeatures\":[{\"type\":\"ecore:EReference\",\"name\":\"links\",\"upperBound\":\"-1\",\"eType\":\"#//Link\",\"containment\":\"true\"},{\"type\":\"ecore:EReference\",\"name\":\"components\",\"upperBound\":\"-1\",\"eType\":\"#//Component\",\"containment\":\"true\"},{\"type\":\"ecore:EReference\",\"name\":\"protocols\",\"upperBound\":\"-1\",\"eType\":\"#//Protocol\",\"containment\":\"true\"}]},{\"type\":\"ecore:EClass\",\"name\":\"Router\",\"eSuperTypes\":\"#//Component\"}]}";
    private static final String ECORE_MODEL2 = "{\"version\":\"2.0\",\"name\":\"model2\",\"nsURI\":\"http://vitruv.tools/methodologisttemplate/model2\",\"nsPrefix\":\"model2\",\"eClassifiers\":[{\"type\":\"ecore:EClass\",\"name\":\"Root\",\"eStructuralFeatures\":[{\"type\":\"ecore:EReference\",\"name\":\"entities\",\"upperBound\":\"-1\",\"eType\":\"#//Entity\",\"containment\":\"true\"},{\"type\":\"ecore:EReference\",\"name\":\"links\",\"upperBound\":\"-1\",\"eType\":\"#//Link\",\"containment\":\"true\"},{\"type\":\"ecore:EReference\",\"name\":\"standards\",\"upperBound\":\"-1\",\"eType\":\"#//CommunicationStandard\",\"containment\":\"true\"}]},{\"type\":\"ecore:EClass\",\"name\":\"Entity\",\"eStructuralFeatures\":{\"type\":\"ecore:EAttribute\",\"name\":\"name\",\"lowerBound\":\"1\",\"eType\":\"ecore:EDataType http://www.eclipse.org/emf/2002/Ecore#//EString\"}},{\"type\":\"ecore:EClass\",\"name\":\"Link\",\"eStructuralFeatures\":[{\"type\":\"ecore:EReference\",\"name\":\"entities\",\"lowerBound\":\"2\",\"upperBound\":\"-1\",\"eType\":\"#//Entity\"},{\"type\":\"ecore:EReference\",\"name\":\"standard\",\"eType\":\"#//CommunicationStandard\"}]},{\"type\":\"ecore:EClass\",\"name\":\"CommunicationStandard\",\"eStructuralFeatures\":{\"type\":\"ecore:EAttribute\",\"name\":\"name\",\"eType\":\"ecore:EDataType http://www.eclipse.org/emf/2002/Ecore#//EString\"}}]}";
}
