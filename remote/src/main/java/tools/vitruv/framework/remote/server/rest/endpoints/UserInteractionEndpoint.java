package tools.vitruv.framework.remote.server.rest.endpoints;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_CONFLICT;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.StreamSupport;
import tools.vitruv.change.interaction.UserInteractionBase;
import tools.vitruv.framework.remote.common.json.JsonMapper;
import tools.vitruv.framework.remote.common.rest.TaskStatus;
import tools.vitruv.framework.remote.server.exception.ServerHaltingException;
import tools.vitruv.framework.remote.server.http.HttpWrapper;
import tools.vitruv.framework.remote.server.propagation.PropagationWorker;
import tools.vitruv.framework.remote.server.rest.PostEndpoint;

/**
 * This endpoint takes answers to the user interactions of a change that is propagated
 * asynchronously, as a JSON array of {@link UserInteractionBase}s, and returns the {@link
 * TaskStatus} of the change afterwards.
 */
public class UserInteractionEndpoint implements PostEndpoint {
  // The property the JSON mapper writes the type of an EObject to.
  private static final String TYPE_PROPERTY = "eClass";
  private static final ObjectMapper SHAPE_READER = new ObjectMapper();

  private final PropagationWorker propagationWorker;
  private final JsonMapper mapper;

  /**
   * Creates a new UserInteractionEndpoint.
   *
   * @param propagationWorker The worker that propagates the changes.
   * @param mapper The JSON mapper to use.
   */
  public UserInteractionEndpoint(PropagationWorker propagationWorker, JsonMapper mapper) {
    this.propagationWorker = propagationWorker;
    this.mapper = mapper;
  }

  @Override
  public String process(HttpWrapper wrapper) {
    int taskId = TaskEndpointSupport.readTaskId(wrapper);
    List<UserInteractionBase> answers = readAnswers(wrapper);
    try {
      propagationWorker.registerUserInteractions(taskId, answers);
    } catch (NoSuchElementException e) {
      throw TaskEndpointSupport.unknownTask(taskId);
    } catch (IllegalStateException e) {
      throw new ServerHaltingException(HTTP_CONFLICT, e.getMessage());
    }
    return TaskEndpointSupport.respondWith(
        propagationWorker.getStatus(taskId).orElseThrow(), wrapper, mapper);
  }

  private List<UserInteractionBase> readAnswers(HttpWrapper wrapper) {
    String body;
    try {
      body = wrapper.getRequestBodyAsString();
    } catch (IOException e) {
      throw internalServerError(e.getMessage());
    }
    checkShapeOfAnswers(body);
    List<UserInteractionBase> answers;
    try {
      answers = mapper.deserializeArrayOf(body, UserInteractionBase.class);
    } catch (IOException | RuntimeException e) {
      throw new ServerHaltingException(HTTP_BAD_REQUEST, "Invalid answers: " + e.getMessage());
    }
    if (answers == null || answers.stream().anyMatch(Objects::isNull)) {
      throw new ServerHaltingException(
          HTTP_BAD_REQUEST, "Expected a JSON array of user interactions");
    }
    return answers;
  }

  /**
   * Rejects bodies that are not an array of objects with an eClass, before they reach the EMF
   * deserializer. For an element without one, it looks for a default type, and on a freshly started
   * server that search does not return for some inputs, e.g., {@code [null]} or {@code [1]}, and
   * blocks the deserialization of later requests as well.
   */
  private static void checkShapeOfAnswers(String body) {
    JsonNode answers;
    try {
      answers = SHAPE_READER.readTree(body);
    } catch (IOException e) {
      throw new ServerHaltingException(HTTP_BAD_REQUEST, "Invalid answers: " + e.getMessage());
    }
    boolean typedObjects =
        answers != null
            && answers.isArray()
            && StreamSupport.stream(answers.spliterator(), false)
                .allMatch(it -> it.isObject() && it.path(TYPE_PROPERTY).isTextual());
    if (!typedObjects) {
      throw new ServerHaltingException(
          HTTP_BAD_REQUEST,
          "Expected a JSON array of user interactions, each with its " + TYPE_PROPERTY);
    }
  }
}
