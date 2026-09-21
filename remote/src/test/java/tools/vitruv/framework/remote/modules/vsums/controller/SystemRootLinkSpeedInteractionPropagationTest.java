package tools.vitruv.framework.remote.modules.vsums.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import tools.vitruv.framework.remote.helper.IntegrationTest;
import tools.vitruv.framework.remote.modules.vsums.async.AsyncTaskState;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Epic 1 Point 3: Link under System (model) → communication speed text input (MBits/s).
 */
@IntegrationTest
@Tag("systemroot-epic1")
class SystemRootLinkSpeedInteractionPropagationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    private SystemRootInteractionPropagationTestSupport support;

    @BeforeEach
    void setUp() {
        support = new SystemRootInteractionPropagationTestSupport(mvc, objectMapper);
    }

    @Test
    void systemLinkInsertPromptsForSpeedInMBitsPerSecond() throws Exception {
        val vsumId = support.createVsum("Link Speed Interaction Test");
        val openedView = support.openViewWithSystem(vsumId);

        val linkInsertBody = """
                [
                  {
                    "uri": "%s",
                    "content": {
                      "eClass": "http://vitruv.tools/methodologisttemplate/model#//System",
                      "_id": "/",
                      "links": [
                        { }
                      ]
                    }
                  }
                ]
                """.formatted(openedView.systemUri());

        val taskId = support.startAsyncUpdate(openedView.viewId(), linkInsertBody);

        val waitingStatus = support.pollUntilState(taskId, AsyncTaskState.WAITING_USER_INTERACTION);
        assertThat(String.valueOf(waitingStatus.interaction().get("eClass")))
                .contains("FreeTextUserInteraction");
        assertThat(waitingStatus.interaction().get("message"))
                .asString()
                .contains("MBits/s");

        val completed = support.submitInteractionAndWaitForCompletion(
                taskId,
                waitingStatus.interaction(),
                Map.of("text", "1000"));

        assertThat(completed.result()).contains("1000");
        assertThat(completed.result()).contains("speedMBitsPerSecond");
    }

    @Test
    void invalidSpeedShowsValidationErrorAndAllowsRetry() throws Exception {
        val vsumId = support.createVsum("Link Speed Validation Retry Test");
        val openedView = support.openViewWithSystem(vsumId);

        val linkInsertBody = """
                [
                  {
                    "uri": "%s",
                    "content": {
                      "eClass": "http://vitruv.tools/methodologisttemplate/model#//System",
                      "_id": "/",
                      "links": [
                        { }
                      ]
                    }
                  }
                ]
                """.formatted(openedView.systemUri());

        val taskId = support.startAsyncUpdate(openedView.viewId(), linkInsertBody);

        val waitingStatus = support.pollUntilState(taskId, AsyncTaskState.WAITING_USER_INTERACTION);
        assertThat(String.valueOf(waitingStatus.interaction().get("eClass")))
                .contains("FreeTextUserInteraction");

        val retryStatus = support.submitInteraction(
                taskId,
                waitingStatus.interaction(),
                Map.of("text", "not-a-number"));

        assertThat(retryStatus.state()).isEqualTo(AsyncTaskState.WAITING_USER_INTERACTION);
        assertThat(retryStatus.interaction()).isNotNull();
        assertThat(retryStatus.interaction().get("validationError"))
                .asString()
                .containsIgnoringCase("positive integer");
        assertThat(String.valueOf(retryStatus.interaction().get("eClass")))
                .contains("FreeTextUserInteraction");

        val completed = support.submitInteractionAndWaitForCompletion(
                taskId,
                retryStatus.interaction(),
                Map.of("text", "1000"));

        assertThat(completed.result()).contains("1000");
        assertThat(completed.result()).contains("speedMBitsPerSecond");
    }
}
