package tools.vitruv.framework.remote.modules.vsums.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import tools.vitruv.framework.remote.modules.vsums.async.AsyncTaskState;
import tools.vitruv.framework.remote.modules.vsums.usecases.dtos.CreateVsumRequestBody;
import tools.vitruv.framework.remote.modules.vsums.usecases.dtos.OpenViewRequestBody;
import tools.vitruv.framework.remote.modules.vsums.usecases.dtos.PropagationTaskStatusResponse;
import tools.vitruv.framework.remote.modules.vsums.usecases.dtos.StartAsyncUpdateResponse;
import tools.vitruv.framework.remote.modules.vsums.usecases.dtos.VsumInfoResponseBody;
import lombok.val;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Shared REST helpers for SystemRootVsum Epic 1 async user-interaction integration tests.
 */
final class SystemRootInteractionPropagationTestSupport {

    record OpenedRootView(UUID viewId, String rootUri) {
    }

    record OpenedSystemView(UUID viewId, String systemUri) {
    }

    private final MockMvc mvc;
    private final ObjectMapper objectMapper;

    SystemRootInteractionPropagationTestSupport(MockMvc mvc, ObjectMapper objectMapper) {
        this.mvc = mvc;
        this.objectMapper = objectMapper;
    }

    UUID createVsum(String description) throws Exception {
        val createVsumRequestBody = objectMapper.writeValueAsString(
                new CreateVsumRequestBody("SystemRootVsum", description, "...")
        );

        val createVsumResponseBody = mvc.perform(post("/v1/vsums")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createVsumRequestBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(createVsumResponseBody, VsumInfoResponseBody.class).id();
    }

    OpenedRootView openViewWithRoot(UUID vsumId) throws Exception {
        val createSelectorResponseBody = mvc.perform(
                        post("/v1/vsums/" + vsumId + "/view-types/default/selectors"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode selector = objectMapper.readTree(createSelectorResponseBody);
        val selectorId = UUID.fromString(selector.get("id").asText());

        int rootIndex = -1;
        int systemIndex = -1;
        for (int i = 0; i < selector.get("selectableObjects").size(); i++) {
            String eClass = selector.get("selectableObjects").get(i).get("eClass").asText();
            if (eClass.contains("model2#//Root")) {
                rootIndex = i;
            } else if (eClass.contains("model#//System")) {
                systemIndex = i;
            }
        }
        assertThat(rootIndex).isGreaterThanOrEqualTo(0);

        UUID[] selectedObjectIds;
        if (systemIndex >= 0) {
            selectedObjectIds = new UUID[]{
                    UUID.fromString(selector.get("selectableObjects").get(rootIndex).get("_id").asText()),
                    UUID.fromString(selector.get("selectableObjects").get(systemIndex).get("_id").asText())
            };
        } else {
            selectedObjectIds = new UUID[]{
                    UUID.fromString(selector.get("selectableObjects").get(rootIndex).get("_id").asText())
            };
        }

        val openViewRequestBody = objectMapper.writeValueAsString(
                new OpenViewRequestBody(vsumId, selectorId, selectedObjectIds)
        );

        val openViewResponseBody = mvc.perform(post("/v1/views")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(openViewRequestBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode view = objectMapper.readTree(openViewResponseBody);
        UUID viewId = UUID.fromString(view.get("id").asText());
        String rootUri = findResourceUri(view, "model2#//Root");
        assertThat(rootUri).isNotNull();

        return new OpenedRootView(viewId, rootUri);
    }

    OpenedSystemView openViewWithSystem(UUID vsumId) throws Exception {
        val createSelectorResponseBody = mvc.perform(
                        post("/v1/vsums/" + vsumId + "/view-types/default/selectors"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode selector = objectMapper.readTree(createSelectorResponseBody);
        val selectorId = UUID.fromString(selector.get("id").asText());

        int systemIndex = -1;
        for (int i = 0; i < selector.get("selectableObjects").size(); i++) {
            String eClass = selector.get("selectableObjects").get(i).get("eClass").asText();
            if (eClass.contains("model#//System")) {
                systemIndex = i;
                break;
            }
        }
        assertThat(systemIndex).isGreaterThanOrEqualTo(0);

        val selectedObjectIds = new UUID[]{
                UUID.fromString(selector.get("selectableObjects").get(systemIndex).get("_id").asText())
        };

        val openViewRequestBody = objectMapper.writeValueAsString(
                new OpenViewRequestBody(vsumId, selectorId, selectedObjectIds)
        );

        val openViewResponseBody = mvc.perform(post("/v1/views")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(openViewRequestBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode view = objectMapper.readTree(openViewResponseBody);
        UUID viewId = UUID.fromString(view.get("id").asText());
        String systemUri = findResourceUri(view, "model#//System");
        assertThat(systemUri).isNotNull();

        return new OpenedSystemView(viewId, systemUri);
    }

    UUID startAsyncUpdate(UUID viewId, String resourceSetBody) throws Exception {
        val asyncStartBody = mvc.perform(post("/v1/views/" + viewId + "/apply-update/async")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resourceSetBody))
                .andExpect(status().isAccepted())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(asyncStartBody, StartAsyncUpdateResponse.class).taskId();
    }

    PropagationTaskStatusResponse submitInteraction(
            UUID taskId, Map<String, Object> interaction, Map<String, Object> responseFields) throws Exception {
        val responsePayload = new HashMap<String, Object>();
        responsePayload.put("eClass", interaction.get("eClass"));
        if (interaction.get("message") != null) {
            responsePayload.put("message", interaction.get("message"));
        }
        responsePayload.putAll(responseFields);
        val responseJson = objectMapper.writeValueAsString(responsePayload);

        mvc.perform(post("/v1/tasks/" + taskId + "/interaction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(responseJson))
                .andExpect(status().isNoContent());

        // After an invalid free-text answer the worker briefly returns to RUNNING, then WAITING again.
        return pollUntilState(taskId, AsyncTaskState.WAITING_USER_INTERACTION);
    }

    PropagationTaskStatusResponse submitInteractionAndWaitForCompletion(
            UUID taskId, Map<String, Object> interaction, Map<String, Object> responseFields) throws Exception {
        val responsePayload = new HashMap<String, Object>();
        responsePayload.put("eClass", interaction.get("eClass"));
        if (interaction.get("message") != null) {
            responsePayload.put("message", interaction.get("message"));
        }
        responsePayload.putAll(responseFields);
        val responseJson = objectMapper.writeValueAsString(responsePayload);

        mvc.perform(post("/v1/tasks/" + taskId + "/interaction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(responseJson))
                .andExpect(status().isNoContent());

        return pollUntilState(taskId, AsyncTaskState.COMPLETED);
    }

    PropagationTaskStatusResponse pollUntilState(UUID taskId, AsyncTaskState expectedState) throws Exception {
        PropagationTaskStatusResponse status = null;
        for (int attempt = 0; attempt < 120; attempt++) {
            val statusBody = mvc.perform(get("/v1/tasks/" + taskId))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            status = objectMapper.readValue(statusBody, PropagationTaskStatusResponse.class);
            if (status.state() == expectedState) {
                return status;
            }
            if (status.state() == AsyncTaskState.FAILED) {
                throw new AssertionError("Propagation failed: " + status.error());
            }
            // Unexpected terminal state while waiting for a dialog.
            if (expectedState == AsyncTaskState.WAITING_USER_INTERACTION
                    && status.state() == AsyncTaskState.COMPLETED) {
                throw new AssertionError(
                        "Task " + taskId + " completed without reaching WAITING_USER_INTERACTION");
            }
            TimeUnit.MILLISECONDS.sleep(100);
        }

        throw new AssertionError(
                "Task " + taskId + " did not reach " + expectedState + " (last: "
                        + (status != null ? status.state() : "unknown") + ")");
    }

    PropagationTaskStatusResponse waitForInteraction(UUID taskId) throws Exception {
        return pollUntilState(taskId, AsyncTaskState.WAITING_USER_INTERACTION);
    }

    private String findResourceUri(JsonNode view, String eClassFragment) {
        for (JsonNode resource : view.get("resourceSet")) {
            String eClass = resource.get("content").get("eClass").asText();
            if (eClass.contains(eClassFragment)) {
                return resource.get("uri").asText();
            }
        }
        return null;
    }
}
