package tools.vitruv.framework.remote.modules.vsums.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.vitruv.framework.remote.helper.IntegrationTest;
import tools.vitruv.framework.remote.modules.vsums.model.manager.VsumManager;
import tools.vitruv.framework.remote.modules.vsums.usecases.dtos.CreateVsumRequestBody;
import tools.vitruv.framework.remote.modules.vsums.usecases.dtos.OpenViewRequestBody;
import tools.vitruv.framework.remote.modules.vsums.usecases.dtos.VsumInfoResponseBody;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class ViewControllerTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private VsumManager vsumManager;


    @Test
    public void test() throws Exception {

        // create vsum
        val vsumId = createVsum();

        val openViewResponseBody = openView(vsumId);
        val view = objectMapper.readTree(openViewResponseBody);
        val viewId = UUID.fromString(view.get("id").asText());
        val normalizedResponseResourceSet = objectMapper.writeValueAsString(view.get("resourceSet"));
        val normalizedExpectedInitial = objectMapper.writeValueAsString(objectMapper.readTree(INITIAL));

        assertThat(normalizedResponseResourceSet).isEqualTo(normalizedExpectedInitial);

        // first commit
        mvc.perform(put("/v1/views/" + viewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(FIRST_UPDATE_REQUEST)
                )
                .andExpect(status().isOk());

        // first update
        val firstUpdateResponseBody = mvc.perform(post("/v1/views/" + viewId + "/apply-update"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        val normalizedActualFirstUpdateResponse =
                objectMapper.writeValueAsString(objectMapper.readTree(firstUpdateResponseBody));
        val normalizedExpectedFirstUpdateResponse =
                objectMapper.writeValueAsString(objectMapper.readTree(FIRST_UPDATE_RESPONSE));
        assertThat(normalizedActualFirstUpdateResponse).isEqualTo(normalizedExpectedFirstUpdateResponse);

        // close view
        mvc.perform(delete("/v1/views/" + viewId))
                .andExpect(status().isOk());

        // check view is closed
        mvc.perform(get("/v1/views/" + viewId))
                .andExpect(status().is4xxClientError());

        // open view again
        val openViewResponseBody2 = openView(vsumId);
        val view2 = objectMapper.readTree(openViewResponseBody2);
        val viewId2 = UUID.fromString(view2.get("id").asText());
        val normalizedResponseResourceSet2 = objectMapper.writeValueAsString(view2.get("resourceSet"));
        assertThat(normalizedResponseResourceSet2).isEqualTo(normalizedExpectedFirstUpdateResponse);

        // close view again
        mvc.perform(delete("/v1/views/" + viewId2))
                .andExpect(status().isOk());

        vsumManager.evictVsum(vsumId);

        val openViewResponseBody3 = openView(vsumId);
        val view3 = objectMapper.readTree(openViewResponseBody3);
        val normalizedResponseResourceSet3 = objectMapper.writeValueAsString(view3.get("resourceSet"));
        assertThat(normalizedResponseResourceSet3).contains(normalizedExpectedFirstUpdateResponse);
    }

    private UUID createVsum() throws Exception {
        val createVsumRequestBody = objectMapper.writeValueAsString(
                new CreateVsumRequestBody("SystemRootVsum", "First Vsum", "...")
        );

        val createVsumResponseBody = mvc.perform(post("/v1/vsums")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createVsumRequestBody)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        val vsumInfo = objectMapper.readValue(createVsumResponseBody, VsumInfoResponseBody.class);
        return vsumInfo.id();
    }

    private String openView(UUID vsumId) throws Exception {
        // create selector
        val createSelectorResponseBody = mvc.perform(post("/v1/vsums/" + vsumId + "/view-types/default/selectors"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        val selector = objectMapper.readTree(createSelectorResponseBody);
        val selectorId = UUID.fromString(selector.get("id").asText());
        val object0EClass = selector.get("selectableObjects").get(0).get("eClass").asText();
        val selectedObjectIndex = object0EClass.contains("System") ? 0 : 1;
        val selectedObjectId = selector.get("selectableObjects").get(selectedObjectIndex).get("_id").asText();

        // open view
        val openViewRequestBody = objectMapper.writeValueAsString(
                new OpenViewRequestBody(vsumId, selectorId, new UUID[]{UUID.fromString(selectedObjectId)})
        );

        return mvc.perform(post("/v1/views")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(openViewRequestBody)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private static final String INITIAL = """
            [ {
              "uri" : "/example.model",
              "content" : {
                "eClass" : "http://vitruv.tools/methodologisttemplate/model#//System",
                "_id" : "/"
              }
            } ]
            """;


    private static final String FIRST_UPDATE_REQUEST = """
            [
               {
                 "uri": "/example.model",
                 "content": {
                   "eClass": "http://vitruv.tools/methodologisttemplate/model#//System",
                   "_id": "/",
                   "components": [
                     {
                       "name": "First Component"
                     },
                     {
                       "name": "Second Component"
                     }
                   ]
                 }
               }
             ]
            """;

    private static final String FIRST_UPDATE_RESPONSE = """
            [
              {
                "uri": "/example.model",
                "content": {
                  "eClass": "http://vitruv.tools/methodologisttemplate/model#//System",
                  "_id": "/",
                  "components": [
                    {
                      "_id": "//@components.0",
                      "name": "First Component"
                    },
                    {
                      "_id": "//@components.1",
                      "name": "Second Component"
                    }
                  ]
                }
              }
            ]
            """;
}
