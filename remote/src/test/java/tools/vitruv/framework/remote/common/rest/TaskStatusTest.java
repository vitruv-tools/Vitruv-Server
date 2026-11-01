package tools.vitruv.framework.remote.common.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.vitruv.change.interaction.PendingUserInteraction;
import tools.vitruv.change.utils.ProjectMarker;
import tools.vitruv.framework.remote.common.json.JsonMapper;

/** Client and server exchange {@link TaskStatus}es through the {@link JsonMapper}. */
class TaskStatusTest {
  @TempDir Path vsumFolder;

  private JsonMapper mapper;

  @BeforeEach
  void createMapper() throws IOException {
    ProjectMarker.markAsProjectRootFolder(vsumFolder);
    mapper = new JsonMapper(vsumFolder);
  }

  @Test
  void statusWithAQuestion_survivesTheRoundTrip() throws Exception {
    var status =
        new TaskStatus(
            12,
            TaskState.WAITING_FOR_INPUT,
            new PendingUserInteraction(
                PendingUserInteraction.Kind.MULTIPLE_SELECTION,
                "Correspondences",
                "Which components should the interface be added to?",
                "OK",
                null,
                "Cancel",
                List.of("Frontend", "Backend")),
            null);

    assertEquals(status, mapper.deserialize(mapper.serialize(status), TaskStatus.class));
  }

  @Test
  void statusWithAnError_survivesTheRoundTrip() throws Exception {
    var status = new TaskStatus(13, TaskState.FAILED, null, "Changes rejected");

    assertEquals(status, mapper.deserialize(mapper.serialize(status), TaskStatus.class));
  }

  @Test
  void json_usesThePlainFieldNames() throws Exception {
    var status =
        new TaskStatus(
            14,
            TaskState.WAITING_FOR_INPUT,
            new PendingUserInteraction(
                PendingUserInteraction.Kind.CONFIRMATION,
                null,
                "Delete it?",
                "Yes",
                "No",
                null,
                null),
            null);

    var json = new ObjectMapper().readTree(mapper.serialize(status));

    assertEquals(14, json.get("taskId").asInt());
    assertEquals("WAITING_FOR_INPUT", json.get("state").asText());
    assertEquals("CONFIRMATION", json.get("question").get("kind").asText());
    assertEquals("Delete it?", json.get("question").get("message").asText());
    assertTrue(json.get("question").get("choices").isEmpty());
    assertTrue(json.get("error").isNull());
  }
}
