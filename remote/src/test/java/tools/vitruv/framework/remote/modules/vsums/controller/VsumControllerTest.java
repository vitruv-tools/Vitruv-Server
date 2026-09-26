package tools.vitruv.framework.remote.modules.vsums.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.vitruv.framework.remote.helper.IntegrationTest;
import tools.vitruv.framework.remote.modules.vsums.usecases.dtos.CreateVsumRequestBody;
import tools.vitruv.framework.remote.modules.vsums.usecases.dtos.UpdateVsumInfoRequestBody;
import tools.vitruv.framework.remote.modules.vsums.usecases.dtos.VsumInfoResponseBody;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class VsumControllerTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void test() throws Exception {

        // no vsums yet
        mvc.perform(get("/v1/vsums"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // create vsum
        val createVsumRequestBody = objectMapper.writeValueAsString(
                new CreateVsumRequestBody("SystemRootVsum", "First Vsum", "...")
        );

        val createVsumResponseBody = mvc.perform(post("/v1/vsums")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createVsumRequestBody)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metamodelName", is("SystemRootVsum")))
                .andExpect(jsonPath("$.name", is("First Vsum")))
                .andExpect(jsonPath("$.description", is("...")))
                .andReturn().getResponse().getContentAsString();

        // has one vsum now
        val vsumInfo = objectMapper.readValue(createVsumResponseBody, VsumInfoResponseBody.class);
        val vsumIdString = vsumInfo.id().toString();

        mvc.perform(get("/v1/vsums"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(vsumIdString)))
                .andExpect(jsonPath("$[0].metamodelName", is(vsumInfo.metamodelName())))
                .andExpect(jsonPath("$[0].name", is(vsumInfo.name())))
                .andExpect(jsonPath("$[0].description", is(vsumInfo.description())));

        // update vsum info
        val updateVsumInfoRequestBody = objectMapper.writeValueAsString(
                new UpdateVsumInfoRequestBody("Updated Vsum Name", "Updated Vsum Description")
        );

        mvc.perform(put("/v1/vsums/" + vsumIdString)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateVsumInfoRequestBody)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(vsumIdString)))
                .andExpect(jsonPath("$.metamodelName", is(vsumInfo.metamodelName())))
                .andExpect(jsonPath("$.name", is("Updated Vsum Name")))
                .andExpect(jsonPath("$.description", is("Updated Vsum Description")));

        // get view types
        mvc.perform(get("/v1/vsums/" + vsumIdString + "/view-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0]", is("default")));

        mvc.perform(post("/v1/vsums/" + vsumIdString + "/view-types/default/selectors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selectableObjects", hasSize(2)))
                .andExpect(jsonPath("$.selectableObjects[0].eClass", startsWith("http://vitruv.tools/methodologisttemplate/model")))
                .andExpect(jsonPath("$.selectableObjects[1].eClass", startsWith("http://vitruv.tools/methodologisttemplate/model")))
                .andReturn().getResponse().getContentAsString();

    }
}
