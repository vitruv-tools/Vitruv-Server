package tools.vitruv.framework.remote.modules.vsums.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.vitruv.framework.remote.helper.IntegrationTest;
import tools.vitruv.framework.remote.modules.vsums.async.AsyncTaskState;
import tools.vitruv.framework.remote.modules.vsums.usecases.dtos.*;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class AsyncPropagationControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void asyncApplyUpdateCompletesWithResult() throws Exception {
        val vsumId = createVsum();
        val viewId = openViewAndGetId(vsumId);

        mvc.perform(put("/v1/views/" + viewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_REQUEST))
                .andExpect(status().isOk());

        val asyncStartBody = mvc.perform(post("/v1/views/" + viewId + "/apply-update/async"))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();

        assertAsyncTaskCompletes(asyncStartBody);
    }

    @Test
    void asyncApplyUpdateWithCommitBodyCompletesWithResult() throws Exception {
        val vsumId = createVsum();
        val viewId = openViewAndGetId(vsumId);

        val asyncStartBody = mvc.perform(post("/v1/views/" + viewId + "/apply-update/async")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_REQUEST))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();

        assertAsyncTaskCompletes(asyncStartBody);
    }

    private void assertAsyncTaskCompletes(String asyncStartBody) throws Exception {
        val startResponse = objectMapper.readValue(asyncStartBody, StartAsyncUpdateResponse.class);
        assertThat(startResponse.taskId()).isNotNull();

        PropagationTaskStatusResponse finalStatus = null;
        for (int attempt = 0; attempt < 50; attempt++) {
            val statusBody = mvc.perform(get("/v1/tasks/" + startResponse.taskId()))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            finalStatus = objectMapper.readValue(statusBody, PropagationTaskStatusResponse.class);
            if (finalStatus.state() == AsyncTaskState.COMPLETED || finalStatus.state() == AsyncTaskState.FAILED) {
                break;
            }
            Thread.sleep(100);
        }

        assertThat(finalStatus).isNotNull();
        assertThat(finalStatus.state()).isEqualTo(AsyncTaskState.COMPLETED);
        assertThat(finalStatus.result()).isNotBlank();
        val resultArray = objectMapper.readTree(finalStatus.result());
        assertThat(resultArray.isArray()).isTrue();
        assertThat(resultArray.size()).isGreaterThan(0);
    }

    private UUID createVsum() throws Exception {
        val createVsumRequestBody = objectMapper.writeValueAsString(
                new CreateVsumRequestBody("SystemRootVsum", "Async Test Vsum", "...")
        );

        val createVsumResponseBody = mvc.perform(post("/v1/vsums")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createVsumRequestBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        val vsumInfo = objectMapper.readValue(createVsumResponseBody, VsumInfoResponseBody.class);
        return vsumInfo.id();
    }

    private UUID openViewAndGetId(UUID vsumId) throws Exception {
        val createSelectorResponseBody = mvc.perform(
                        post("/v1/vsums/" + vsumId + "/view-types/default/selectors"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        val selector = objectMapper.readTree(createSelectorResponseBody);
        val selectorId = UUID.fromString(selector.get("id").asText());
        val object0EClass = selector.get("selectableObjects").get(0).get("eClass").asText();
        val selectedObjectIndex = object0EClass.contains("System") ? 0 : 1;
        val selectedObjectId = selector.get("selectableObjects").get(selectedObjectIndex).get("_id").asText();

        val openViewRequestBody = objectMapper.writeValueAsString(
                new OpenViewRequestBody(vsumId, selectorId, new UUID[]{UUID.fromString(selectedObjectId)})
        );

        val openViewResponseBody = mvc.perform(post("/v1/views")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(openViewRequestBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        val view = objectMapper.readTree(openViewResponseBody);
        return UUID.fromString(view.get("id").asText());
    }

    private static final String UPDATE_REQUEST = """
            [
               {
                 "uri": "/example.model",
                 "content": {
                   "eClass": "http://vitruv.tools/methodologisttemplate/model#//System",
                   "_id": "/",
                   "components": [
                     {
                       "name": "First Component"
                     }
                   ]
                 }
               }
             ]
            """;
}
