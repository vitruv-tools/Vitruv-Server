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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Entity insert under Root prompts for component type. */
@IntegrationTest
@Tag("systemroot-epic1")
class SystemRootEntityInteractionPropagationTest {

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
    void entityInsertInRootPromptsForComponentType() throws Exception {
        val vsumId = support.createVsum("Entity Interaction Test");
        val openedView = support.openViewWithRoot(vsumId);

        val entityInsertBody = """
                [
                  {
                    "uri": "%s",
                    "content": {
                      "eClass": "http://vitruv.tools/methodologisttemplate/model2#//Root",
                      "_id": "/",
                      "entities": [
                        { "name": "newEntity" }
                      ]
                    }
                  }
                ]
                """.formatted(openedView.rootUri());

        val taskId = support.startAsyncUpdate(openedView.viewId(), entityInsertBody);

        val waitingStatus = support.pollUntilState(taskId, AsyncTaskState.WAITING_USER_INTERACTION);
        assertThat(waitingStatus.interaction()).isNotNull();
        assertThat(String.valueOf(waitingStatus.interaction().get("eClass")))
                .contains("MultipleChoiceSingleSelectionUserInteraction");
        assertThat(waitingStatus.interaction().get("message"))
                .asString()
                .contains("newEntity");

        @SuppressWarnings("unchecked")
        val choices = (List<String>) waitingStatus.interaction().get("choices");
        assertThat(choices).containsExactly("Plain Component", "Server", "Device");

        val completed = support.submitInteractionAndWaitForCompletion(
                taskId,
                waitingStatus.interaction(),
                Map.of("selectedIndex", 1));

        assertThat(completed.result()).contains("newEntity");
        assertThat(completed.result()).contains("Server");
    }
}
