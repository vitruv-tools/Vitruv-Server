package tools.vitruv.framework.remote.server.propagation;

/**
 * Thrown by {@link PropagationWorker#awaitFinishedSubmissions} when a change that was submitted for
 * the view waits for user input, so waiting for it would mean waiting for a person.
 */
public class WaitingForInputException extends Exception {
  private final int taskId;

  /**
   * Creates the exception for the change submitted under the given task id.
   *
   * @param taskId the task id of the change that waits for user input
   */
  public WaitingForInputException(int taskId) {
    super("The change with task id " + taskId + " waits for user input");
    this.taskId = taskId;
  }

  /**
   * Returns the task id of the change that waits for user input.
   *
   * @return the task id returned by {@link PropagationWorker#submit}
   */
  public int getTaskId() {
    return taskId;
  }
}
