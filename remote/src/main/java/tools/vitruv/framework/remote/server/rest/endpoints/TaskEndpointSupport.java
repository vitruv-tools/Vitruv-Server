package tools.vitruv.framework.remote.server.rest.endpoints;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;

import com.fasterxml.jackson.core.JsonProcessingException;
import tools.vitruv.framework.remote.common.json.JsonMapper;
import tools.vitruv.framework.remote.common.rest.TaskStatus;
import tools.vitruv.framework.remote.common.rest.constants.ContentType;
import tools.vitruv.framework.remote.common.rest.constants.Header;
import tools.vitruv.framework.remote.server.exception.ServerHaltingException;
import tools.vitruv.framework.remote.server.http.HttpWrapper;

/** Request and response handling shared by the endpoints for asynchronously propagated changes. */
final class TaskEndpointSupport {
  private TaskEndpointSupport() {}

  /**
   * Reads the task id from the {@link Header#TASK_ID} header.
   *
   * @param wrapper the request
   * @return the task id
   * @throws ServerHaltingException with status 400 if the header is missing or not a number
   */
  static int readTaskId(HttpWrapper wrapper) {
    String taskId = wrapper.getRequestHeader(Header.TASK_ID);
    if (taskId == null) {
      throw new ServerHaltingException(HTTP_BAD_REQUEST, "Header " + Header.TASK_ID + " missing!");
    }
    try {
      return Integer.parseInt(taskId.trim());
    } catch (NumberFormatException e) {
      throw new ServerHaltingException(HTTP_BAD_REQUEST, "Invalid task id: " + taskId);
    }
  }

  /**
   * Creates the exception for a task id under which no change was submitted.
   *
   * @param taskId the unknown task id
   * @return an exception with status 404
   */
  static ServerHaltingException unknownTask(int taskId) {
    return new ServerHaltingException(HTTP_NOT_FOUND, "Task with id " + taskId + " not found!");
  }

  /**
   * Turns the status into the JSON body of the response.
   *
   * @param status the status to send
   * @param wrapper the response
   * @param mapper the JSON mapper to use
   * @return the response body
   */
  static String respondWith(TaskStatus status, HttpWrapper wrapper, JsonMapper mapper) {
    try {
      String body = mapper.serialize(status);
      wrapper.setContentType(ContentType.APPLICATION_JSON);
      return body;
    } catch (JsonProcessingException e) {
      throw new ServerHaltingException(HTTP_INTERNAL_ERROR, e.getMessage());
    }
  }
}
