package tools.vitruv.framework.remote.server.registry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for managing asynchronous task executions.
 * Tracks task IDs and their current status for long-running operations.
 */
public class AsyncTaskRegistry {
    private static final AsyncTaskRegistry INSTANCE = new AsyncTaskRegistry();
    private final Map<String, AsyncTaskStatus> tasks = new ConcurrentHashMap<>();

    private AsyncTaskRegistry() {
    }

    public static AsyncTaskRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Registers a new task in the registry.
     * 
     * @param taskId the unique task identifier
     * @return the AsyncTaskStatus for this task
     */
    public AsyncTaskStatus registerTask(String taskId) {
        AsyncTaskStatus status = new AsyncTaskStatus(taskId);
        tasks.put(taskId, status);
        return status;
    }

    /**
     * Retrieves the status of a task by its ID.
     * 
     * @param taskId the unique task identifier
     * @return the AsyncTaskStatus or null if task not found
     */
    public AsyncTaskStatus getStatus(String taskId) {
        return tasks.get(taskId);
    }

    /**
     * Marks a task as completed with a result.
     * 
     * @param taskId the unique task identifier
     * @param result the result of the task execution
     */
    public void completeTask(String taskId, Object result) {
        AsyncTaskStatus status = tasks.get(taskId);
        if (status != null) {
            status.complete(result);
        }
    }

    /**
     * Marks a task as failed with an error message.
     * 
     * @param taskId       the unique task identifier
     * @param errorMessage the error message
     */
    public void failTask(String taskId, String errorMessage) {
        AsyncTaskStatus status = tasks.get(taskId);
        if (status != null) {
            status.fail(errorMessage);
        }
    }

    /**
     * Sets a task to waiting for user interaction state.
     * 
     * @param taskId the unique task identifier
     */
    public void setTaskWaitingForUserInteraction(String taskId) {
        AsyncTaskStatus status = tasks.get(taskId);
        if (status != null) {
            status.waitForUserInteraction();
        }
    }

    /**
     * Removes a task from the registry.
     * 
     * @param taskId the unique task identifier
     */
    public void removeTask(String taskId) {
        tasks.remove(taskId);
    }

    /**
     * Checks if a task exists in the registry.
     * 
     * @param taskId the unique task identifier
     * @return true if the task exists, false otherwise
     */
    public boolean taskExists(String taskId) {
        return tasks.containsKey(taskId);
    }
}
