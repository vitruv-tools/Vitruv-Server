package tools.vitruv.framework.remote.modules.vsums.async;

import com.fasterxml.jackson.databind.ObjectMapper;
import tools.vitruv.framework.remote.helper.IntegrationTest;
import tools.vitruv.framework.remote.modules.vsums.usecases.InconsistencyUseCases;
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
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Epic 2 bridge: park waiting task → Hub list → resolve via interaction → RESOLVED;
 * one OPEN per VSUM; async update blocked while OPEN.
 */
@IntegrationTest
class InconsistencyParkResolveIntegrationTest {

    private static final String CONFIRMATION_MESSAGE = "Hub park resolve test?";
    private static final String CONFIRMED_RESULT = "[{\"uri\":\"/park-resolve-test\"}]";

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    PropagationTaskRegistry taskRegistry;

    @Autowired
    ServerInteractionResultProvider interactionResultProvider;

    @Autowired
    InconsistencyUseCases inconsistencyUseCases;

    @Test
    void parkResolveAndCommentRoundTrip() throws Exception {
        val viewId = openViewAndGetId(createVsum());
        val taskId = UUID.randomUUID();
        taskRegistry.register(taskId, viewId);

        val worker = new Thread(() -> runConfirmationWorker(taskId));
        worker.start();

        pollUntilState(taskId, AsyncTaskState.WAITING_USER_INTERACTION);

        val parkBody = mvc.perform(post("/v1/tasks/" + taskId + "/inconsistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("OPEN"))
                .andExpect(jsonPath("$.taskId").value(taskId.toString()))
                .andReturn().getResponse().getContentAsString();

        val inconsistencyId = objectMapper.readTree(parkBody).get("id").asText();

        mvc.perform(get("/v1/inconsistencies/" + inconsistencyId + "/model"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.encodedResourceSet").isNotEmpty());

        mvc.perform(get("/v1/inconsistencies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(inconsistencyId));

        mvc.perform(post("/v1/inconsistencies/" + inconsistencyId + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"author":"demo","body":"Resolve from Hub"}
                                """))
                .andExpect(status().isCreated());

        mvc.perform(get("/v1/inconsistencies/" + inconsistencyId + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].body").value("Resolve from Hub"));

        val waitingStatus = pollUntilState(taskId, AsyncTaskState.WAITING_USER_INTERACTION);
        val responsePayload = new HashMap<>(waitingStatus.interaction());
        responsePayload.put("confirmed", true);
        val responseJson = objectMapper.writeValueAsString(responsePayload);

        mvc.perform(post("/v1/tasks/" + taskId + "/interaction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(responseJson))
                .andExpect(status().isNoContent());

        worker.join(10_000);
        assertThat(worker.isAlive()).isFalse();

        pollUntilState(taskId, AsyncTaskState.COMPLETED);
        inconsistencyUseCases.markResolvedByTaskId(taskId);

        mvc.perform(get("/v1/inconsistencies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mvc.perform(get("/v1/inconsistencies").param("state", "RESOLVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(inconsistencyId))
                .andExpect(jsonPath("$[0].state").value("RESOLVED"));
    }

    @Test
    void secondOpenInconsistencyForSameVsumIsRejected() throws Exception {
        val viewId = openViewAndGetId(createVsum());
        val taskId1 = UUID.randomUUID();
        val taskId2 = UUID.randomUUID();
        taskRegistry.register(taskId1, viewId);
        taskRegistry.register(taskId2, viewId);

        val worker1 = new Thread(() -> runConfirmationWorker(taskId1));
        worker1.start();
        pollUntilState(taskId1, AsyncTaskState.WAITING_USER_INTERACTION);

        mvc.perform(post("/v1/tasks/" + taskId1 + "/inconsistent"))
                .andExpect(status().isOk());

        val worker2 = new Thread(() -> runConfirmationWorker(taskId2));
        worker2.start();
        pollUntilState(taskId2, AsyncTaskState.WAITING_USER_INTERACTION);

        mvc.perform(post("/v1/tasks/" + taskId2 + "/inconsistent"))
                .andExpect(status().isConflict());

        taskRegistry.markInconsistent(taskId1);
        taskRegistry.markInconsistent(taskId2);
        worker1.join(2_000);
        worker2.join(2_000);
    }

    @Test
    void asyncUpdateBlockedWhileOpenInconsistencyExists() throws Exception {
        val viewId = openViewAndGetId(createVsum());
        val taskId = UUID.randomUUID();
        taskRegistry.register(taskId, viewId);

        val worker = new Thread(() -> runConfirmationWorker(taskId));
        worker.start();
        pollUntilState(taskId, AsyncTaskState.WAITING_USER_INTERACTION);

        mvc.perform(post("/v1/tasks/" + taskId + "/inconsistent"))
                .andExpect(status().isOk());

        mvc.perform(post("/v1/views/" + viewId + "/apply-update/async")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());

        taskRegistry.markInconsistent(taskId);
        worker.join(2_000);
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
            taskRegistry.completeTask(taskId, confirmed ? CONFIRMED_RESULT : "[]");
        } catch (PropagationInconsistentException e) {
            taskRegistry.markInconsistent(taskId);
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
                new CreateVsumRequestBody("SystemRootVsum", "Park Hub Test Vsum", "...")
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
