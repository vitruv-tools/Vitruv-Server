package tools.vitruv.framework.remote.modules.vsums.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import tools.vitruv.framework.remote.helper.IntegrationTest;
import tools.vitruv.framework.remote.modules.vsums.async.AsyncTaskState;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Link insert under Root prompts for protocol assignment. */
@IntegrationTest
@Tag("systemroot-epic1")
class SystemRootLinkProtocolInteractionPropagationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    private tools.vitruv.framework.remote.modules.vsums.controller.SystemRootInteractionPropagationTestSupport support;

    @BeforeEach
    void setUp() {
        support = new tools.vitruv.framework.remote.modules.vsums.controller.SystemRootInteractionPropagationTestSupport(mvc, objectMapper);
    }

    @Test
    void rootLinkInsertWithNoProtocolsPromptsForNewProtocolName() throws Exception {
        val vsumId = support.createVsum("Link Protocol Interaction Test");
        val openedView = support.openViewWithRoot(vsumId);

        val taskId = support.startAsyncUpdate(openedView.viewId(), rootLinkInsertBody(openedView.rootUri()));

        val waitingStatus = support.pollUntilState(taskId, AsyncTaskState.WAITING_USER_INTERACTION);
        assertThat(String.valueOf(waitingStatus.interaction().get("eClass")))
                .contains("FreeTextUserInteraction");
        assertThat(waitingStatus.interaction().get("message"))
                .asString()
                .containsIgnoringCase("protocol");

        val completed = support.submitInteractionAndWaitForCompletion(
                taskId,
                waitingStatus.interaction(),
                Map.of("text", "Ethernet"));

        assertThat(completed.result()).contains("Ethernet");
        assertThat(completed.result()).contains("standard");
    }

    private static String rootLinkInsertBody(String rootUri) {
        return """
                [
                  {
                    "uri": "%s",
                    "content": {
                      "eClass": "http://vitruv.tools/methodologisttemplate/model2#//Root",
                      "_id": "/",
                      "links": [
                        { }
                      ]
                    }
                  }
                ]
                """.formatted(rootUri);
    }
}
