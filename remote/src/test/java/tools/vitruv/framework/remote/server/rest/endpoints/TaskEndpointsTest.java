package tools.vitruv.framework.remote.server.rest.endpoints;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import tools.vitruv.change.interaction.ConfirmationUserInteraction;
import tools.vitruv.change.interaction.InteractionFactory;
import tools.vitruv.change.interaction.PendingUserInteraction;
import tools.vitruv.change.interaction.UserInteractionBase;
import tools.vitruv.change.utils.ProjectMarker;
import tools.vitruv.framework.remote.common.json.JsonMapper;
import tools.vitruv.framework.remote.common.rest.TaskState;
import tools.vitruv.framework.remote.common.rest.TaskStatus;
import tools.vitruv.framework.remote.common.rest.constants.ContentType;
import tools.vitruv.framework.remote.common.rest.constants.Header;
import tools.vitruv.framework.remote.server.exception.ServerHaltingException;
import tools.vitruv.framework.remote.server.propagation.PropagationWorker;

/** Tests the endpoints at {@code /vsum/task}: GET for the status, POST for answers. */
class TaskEndpointsTest {
  private static final int TASK_ID = 7;
  private static final TaskStatus WAITING =
      new TaskStatus(
          TASK_ID,
          TaskState.WAITING_FOR_INPUT,
          new PendingUserInteraction(
              PendingUserInteraction.Kind.CONFIRMATION,
              null,
              "Delete the class as well?",
              "Yes",
              "No",
              null,
              List.of()),
          null);

  @TempDir Path vsumFolder;

  private final PropagationWorker worker = mock(PropagationWorker.class);
  private JsonMapper mapper;
  private TaskStatusEndpoint statusEndpoint;
  private UserInteractionEndpoint answerEndpoint;

  @BeforeEach
  void setUp() throws IOException {
    ProjectMarker.markAsProjectRootFolder(vsumFolder);
    mapper = new JsonMapper(vsumFolder);
    statusEndpoint = new TaskStatusEndpoint(worker, mapper);
    answerEndpoint = new UserInteractionEndpoint(worker, mapper);
  }

  @Test
  void get_returnsTheStatusAsJson() throws Exception {
    when(worker.getStatus(TASK_ID)).thenReturn(Optional.of(WAITING));
    var wrapper = new FakeHttpWrapper(null).withHeader(Header.TASK_ID, "7");

    String body = statusEndpoint.process(wrapper);

    assertEquals(WAITING, mapper.deserialize(body, TaskStatus.class));
    assertEquals(HttpURLConnection.HTTP_OK, wrapper.responseCode());
    assertEquals(ContentType.APPLICATION_JSON, wrapper.contentType());
  }

  @Test
  void get_withoutTaskId_isABadRequest() {
    assertStatusCode(
        HttpURLConnection.HTTP_BAD_REQUEST,
        () -> statusEndpoint.process(new FakeHttpWrapper(null)));
  }

  @Test
  void get_withANonNumericTaskId_isABadRequest() {
    var wrapper = new FakeHttpWrapper(null).withHeader(Header.TASK_ID, "seven");

    assertStatusCode(HttpURLConnection.HTTP_BAD_REQUEST, () -> statusEndpoint.process(wrapper));
  }

  @Test
  void get_ofAnUnknownTask_isNotFound() {
    when(worker.getStatus(TASK_ID)).thenReturn(Optional.empty());
    var wrapper = new FakeHttpWrapper(null).withHeader(Header.TASK_ID, "7");

    assertStatusCode(HttpURLConnection.HTTP_NOT_FOUND, () -> statusEndpoint.process(wrapper));
  }

  @Test
  void post_registersTheAnswersAndReturnsTheStatus() throws Exception {
    var queued = new TaskStatus(TASK_ID, TaskState.QUEUED, null, null);
    when(worker.getStatus(TASK_ID)).thenReturn(Optional.of(queued));
    ConfirmationUserInteraction confirmation =
        InteractionFactory.eINSTANCE.createConfirmationUserInteraction();
    confirmation.setMessage("Delete the class as well?");
    confirmation.setConfirmed(true);
    var wrapper =
        new FakeHttpWrapper(mapper.serialize(List.of(confirmation)))
            .withHeader(Header.TASK_ID, "7");

    String body = answerEndpoint.process(wrapper);

    assertEquals(queued, mapper.deserialize(body, TaskStatus.class));
    assertEquals(HttpURLConnection.HTTP_OK, wrapper.responseCode());
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<UserInteractionBase>> answers = ArgumentCaptor.forClass(List.class);
    verify(worker).registerUserInteractions(eq(TASK_ID), answers.capture());
    assertEquals(1, answers.getValue().size());
    var answer = assertInstanceOf(ConfirmationUserInteraction.class, answers.getValue().get(0));
    assertEquals("Delete the class as well?", answer.getMessage());
    assertTrue(answer.isConfirmed());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "",
        "{",
        "null",
        "[null]",
        "[1]",
        "[\"yes\"]",
        "[[]]",
        "[{}]",
        "{\"confirmed\": true}",
        "[{\"confirmed\": true}]"
      })
  void post_withAnInvalidBody_isABadRequest(String body) {
    var wrapper = new FakeHttpWrapper(body).withHeader(Header.TASK_ID, "7");

    assertStatusCode(HttpURLConnection.HTTP_BAD_REQUEST, () -> answerEndpoint.process(wrapper));
    verify(worker, never()).registerUserInteractions(anyInt(), anyList());
  }

  @Test
  void post_withANonNumericTaskId_isABadRequest() {
    var wrapper = new FakeHttpWrapper("[]").withHeader(Header.TASK_ID, "x");

    assertStatusCode(HttpURLConnection.HTTP_BAD_REQUEST, () -> answerEndpoint.process(wrapper));
  }

  @Test
  void post_forAnUnknownTask_isNotFound() {
    doThrow(new NoSuchElementException("unknown"))
        .when(worker)
        .registerUserInteractions(eq(TASK_ID), anyList());
    var wrapper = new FakeHttpWrapper("[]").withHeader(Header.TASK_ID, "7");

    assertStatusCode(HttpURLConnection.HTTP_NOT_FOUND, () -> answerEndpoint.process(wrapper));
  }

  @Test
  void post_forAFinishedTask_isAConflict() {
    doThrow(new IllegalStateException("already done"))
        .when(worker)
        .registerUserInteractions(eq(TASK_ID), anyList());
    var wrapper = new FakeHttpWrapper("[]").withHeader(Header.TASK_ID, "7");

    assertStatusCode(HttpURLConnection.HTTP_CONFLICT, () -> answerEndpoint.process(wrapper));
  }

  private static void assertStatusCode(int expected, Runnable request) {
    var exception = assertThrows(ServerHaltingException.class, request::run);
    assertEquals(expected, exception.getStatusCode(), exception.getMessage());
  }
}
