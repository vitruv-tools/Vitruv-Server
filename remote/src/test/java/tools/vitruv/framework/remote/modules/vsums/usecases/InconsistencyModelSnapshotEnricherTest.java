package tools.vitruv.framework.remote.modules.vsums.usecases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.vitruv.framework.remote.modules.vsums.usecases.InconsistencyModelSnapshotEnricher;

import static org.assertj.core.api.Assertions.assertThat;

class InconsistencyModelSnapshotEnricherTest {

    private InconsistencyModelSnapshotEnricher enricher;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        enricher = new InconsistencyModelSnapshotEnricher(objectMapper);
    }

    @Test
    void addsMissingEntityNamedInMessage() throws Exception {
        String encoded = """
                [
                  {
                    "uri": "file:/example.model2",
                    "content": {
                      "eClass": "http://vitruv.tools/methodologisttemplate/model2#//Root",
                      "_id": "/",
                      "entities": [
                        { "name": "testt" },
                        { "name": "test22" }
                      ]
                    }
                  }
                ]
                """;
        String message = "Select the component type to create in the System model for entity 'entityabhishek'.";

        String enriched = enricher.enrich(encoded, message);

        JsonNode root = objectMapper.readTree(enriched).get(0).get("content");
        assertThat(root.get("entities")).hasSize(3);
        assertThat(root.get("entities").get(2).get("name").asText()).isEqualTo("entityabhishek");
    }

    @Test
    void leavesSnapshotUnchangedWhenEntityAlreadyPresent() {
        String encoded = """
                [
                  {
                    "content": {
                      "eClass": "http://vitruv.tools/methodologisttemplate/model2#//Root",
                      "entities": [ { "name": "entityabhishek" } ]
                    }
                  }
                ]
                """;
        String message = "Select the component type for entity 'entityabhishek'.";

        assertThat(enricher.enrich(encoded, message)).isEqualTo(encoded);
    }

    @Test
    void returnsOriginalWhenMessageHasNoEntityName() {
        String encoded = "[{\"content\":{\"eClass\":\"http://vitruv.tools/methodologisttemplate/model2#//Root\"}}]";

        assertThat(enricher.enrich(encoded, "No protocols exist yet.")).isEqualTo(encoded);
    }
}
