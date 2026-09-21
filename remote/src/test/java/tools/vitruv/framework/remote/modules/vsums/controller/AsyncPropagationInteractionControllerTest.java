package tools.vitruv.framework.remote.modules.vsums.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import tools.vitruv.framework.remote.helper.IntegrationTest;
import tools.vitruv.framework.remote.modules.vsums.async.AsyncTaskState;
import tools.vitruv.framework.remote.modules.vsums.async.PropagationTaskRegistry;
import tools.vitruv.framework.remote.modules.vsums.async.ServerInteractionResultProvider;
import tools.vitruv.framework.remote.modules.vsums.usecases.dtos.CreateVsumRequestBody;
import tools.vitruv.framework.remote.modules.vsums.usecases.dtos.OpenViewRequestBody;
import tools.vitruv.framework.remote.modules.vsums.usecases.dtos.PropagationTaskStatusResponse;
import tools.vitruv.framework.remote.modules.vsums.usecases.dtos.VsumInfoResponseBody;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the {@link AsyncTaskState#WAITING_USER_INTERACTION} path via REST.
 * SystemRootVsum does not prompt during real propagation, so a worker thread calls
 * {@link ServerInteractionResultProvider} directly while the test drives GET/POST task APIs.
 */
@IntegrationTest
class AsyncPropagationInteractionControllerTest {

    private static final String CONFIRMED_RESULT = "[{\"uri\":\"/interaction-test\"}]";
    private static final String DECLINED_RESULT = "[]";
    private static final String CONFIRMATION_MESSAGE = "Do you want to propagate this change?";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PropagationTaskRegistry taskRegistry;

    @Autowired
    private ServerInteractionResultProvider interactionResultProvider;

    @Test
    void userInteractionRoundTripThroughRestApi() throws Exception {
        val viewId = openViewAndGetId(createVsum());
        val taskId = UUID.randomUUID();
        taskRegistry.register(taskId, viewId);

        val worker = new Thread(() -> runConfirmationWorker(taskId));
        worker.start();

        val waitingStatus = pollUntilState(taskId, AsyncTaskState.WAITING_USER_INTERACTION);
        assertThat(waitingStatus.interaction()).isNotNull();
        assertThat(waitingStatus.interaction().get("message")).isEqualTo(CONFIRMATION_MESSAGE);
        assertThat(String.valueOf(waitingStatus.interaction().get("eClass")))
                .contains("ConfirmationUserInteraction");

        val responsePayload = new HashMap<>(waitingStatus.interaction());
        responsePayload.put("confirmed", true);
        val responseJson = objectMapper.writeValueAsString(responsePayload);

        mvc.perform(post("/v1/tasks/" + taskId + "/interaction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(responseJson))
                .andExpect(status().isNoContent());

        worker.join(10_000);
        assertThat(worker.isAlive()).isFalse();

        val finalStatus = pollUntilState(taskId, AsyncTaskState.COMPLETED);
        assertThat(finalStatus.result()).isEqualTo(CONFIRMED_RESULT);
    }

    @Test
    void declinedConfirmationCompletesWithAlternateResult() throws Exception {
        val viewId = openViewAndGetId(createVsum());
        val taskId = UUID.randomUUID();
        taskRegistry.register(taskId, viewId);

        val worker = new Thread(() -> runConfirmationWorker(taskId));
        worker.start();

        val waitingStatus = pollUntilState(taskId, AsyncTaskState.WAITING_USER_INTERACTION);

        val responsePayload = new HashMap<>(waitingStatus.interaction());
        responsePayload.put("confirmed", false);
        val responseJson = objectMapper.writeValueAsString(responsePayload);

        mvc.perform(post("/v1/tasks/" + taskId + "/interaction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(responseJson))
                .andExpect(status().isNoContent());

        worker.join(10_000);
        assertThat(worker.isAlive()).isFalse();

        val finalStatus = pollUntilState(taskId, AsyncTaskState.COMPLETED);
        assertThat(finalStatus.result()).isEqualTo(DECLINED_RESULT);
    }

    @Test
    void submitInteractionWhenNotWaitingReturnsConflict() throws Exception {
        val viewId = openViewAndGetId(createVsum());
        val taskId = UUID.randomUUID();
        taskRegistry.register(taskId, viewId);

        val interactionJson = """
                {
                  "eClass": "http://vitruv.tools/change/interaction#//ConfirmationUserInteraction",
                  "message": "Proceed?",
                  "confirmed": true
                }
                """;

        mvc.perform(post("/v1/tasks/" + taskId + "/interaction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(interactionJson))
                .andExpect(status().isConflict());
    }

    private void runConfirmationWorker(UUID taskId) {
        ServerInteractionResultProvider.setCurrentTaskId(taskId);
        try {
            boolean confirmed = interactionResultProvider.getConfirmationInteractionResult(
                    null,
                    "Apply change",
                    CONFIRMATION_MESSAGE,
                    "Yes",
                    "No",
                    "Cancel");
            taskRegistry.completeTask(taskId, confirmed ? CONFIRMED_RESULT : DECLINED_RESULT);
        } finally {
            ServerInteractionResultProvider.clearCurrentTaskId();
        }
    }

    private PropagationTaskStatusResponse pollUntilState(UUID taskId, AsyncTaskState expectedState)
            throws Exception {
        PropagationTaskStatusResponse status = null;
        for (int attempt = 0; attempt < 50; attempt++) {
            val statusBody = mvc.perform(get("/v1/tasks/" + taskId))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            status = objectMapper.readValue(statusBody, PropagationTaskStatusResponse.class);
            if (status.state() == expectedState) {
                return status;
            }
            TimeUnit.MILLISECONDS.sleep(100);
        }

        throw new AssertionError(
                "Task " + taskId + " did not reach state " + expectedState + " (last: "
                        + (status != null ? status.state() : "unknown") + ")");
    }

    private UUID createVsum() throws Exception {
        val createVsumRequestBody = objectMapper.writeValueAsString(
                new CreateVsumRequestBody("SystemRootVsum", "Interaction Test Vsum", "...")
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
}
