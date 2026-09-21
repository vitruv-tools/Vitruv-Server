package tools.vitruv.framework.remote.modules.vsums.async;

import tools.vitruv.framework.remote.helper.IntegrationTest;
import tools.vitruv.framework.remote.modules.vsums.model.entities.OpenInconsistency;
import tools.vitruv.framework.remote.modules.vsums.model.entities.OpenInconsistencyRepo;
import tools.vitruv.framework.remote.modules.vsums.model.entities.OpenInconsistencyState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class InconsistencyCatalogIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    OpenInconsistencyRepo openInconsistencyRepo;

    @Autowired
    PropagationTaskRegistry taskRegistry;

    @Test
    void listFiltersByStateAndSupportsCommentsAndViewUpdates() throws Exception {
        OpenInconsistency open = save("Open title", OpenInconsistencyState.OPEN);
        taskRegistry.register(open.getTaskId(), open.getViewId());
        OpenInconsistency resolved = save("Resolved title", OpenInconsistencyState.RESOLVED);
        resolved.setResolvedAt(Instant.now());
        openInconsistencyRepo.save(resolved);

        mockMvc.perform(get("/v1/inconsistencies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(open.getId().toString()))
                .andExpect(jsonPath("$[0].state").value("OPEN"));

        mockMvc.perform(get("/v1/inconsistencies").param("state", "RESOLVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(resolved.getId().toString()));

        mockMvc.perform(get("/v1/inconsistencies").param("state", "ALL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(post("/v1/inconsistencies/" + open.getId() + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"author":"demo","body":"Need to pick Device"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.author").value("demo"))
                .andExpect(jsonPath("$.body").value("Need to pick Device"));

        mockMvc.perform(get("/v1/inconsistencies/" + open.getId() + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].body").value("Need to pick Device"));

        mockMvc.perform(get("/v1/inconsistencies/" + open.getId() + "/view-updates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(post("/v1/inconsistencies/" + resolved.getId() + "/resolution")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resolvedBy":"demo","choice":"linkComponent","comment":"Looks correct"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resolvedBy").value("demo"))
                .andExpect(jsonPath("$.resolutionChoice").value("linkComponent"))
                .andExpect(jsonPath("$.resolutionComment").value("Looks correct"));
    }

    private OpenInconsistency save(String title, OpenInconsistencyState state) {
        OpenInconsistency entity = new OpenInconsistency();
        entity.setVsumId(UUID.randomUUID());
        entity.setTaskId(UUID.randomUUID());
        entity.setViewId(UUID.randomUUID());
        entity.setVsumName("Test VSUM");
        entity.setTitle(title);
        entity.setMessage(title);
        entity.setInteractionJson("{\"message\":\"" + title + "\"}");
        entity.setState(state);
        entity.setCreatedAt(Instant.now());
        return openInconsistencyRepo.save(entity);
    }
}
