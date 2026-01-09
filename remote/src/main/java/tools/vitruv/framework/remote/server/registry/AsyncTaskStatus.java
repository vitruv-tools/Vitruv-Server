package tools.vitruv.framework.remote.server.registry;

import java.time.LocalDateTime;

public class AsyncTaskStatus {
    private final String taskId;
    private volatile TaskState state;
    private final LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private Object result;
    private String errorMessage;

    public enum TaskState {
        RUNNING, WAITING_USER_INTERACTION, COMPLETED, FAILED
    }

    public AsyncTaskStatus(String taskId) {
        this.taskId = taskId;
        this.state = TaskState.RUNNING;
        this.createdAt = LocalDateTime.now();
    }

    public String getTaskId() {
        return taskId;
    }

    public TaskState getState() {
        return state;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public Object getResult() {
        return result;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void complete(Object result) {
        this.result = result;
        this.completedAt = LocalDateTime.now();
    }

    public void fail(String errorMessage) {
        this.errorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
    }

    public void waitForUserInteraction() {
        this.state = TaskState.WAITING_USER_INTERACTION;
    }

    public boolean isRunning() {
        return state == TaskState.RUNNING;
    }

    public boolean isWaitingForUserInteraction() {
        return state == TaskState.WAITING_USER_INTERACTION;
    }

    public boolean isCompleted() {
        return state == TaskState.COMPLETED || state == TaskState.FAILED;
    }
}
