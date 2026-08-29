package tools.vitruv.framework.remote.server.registry;

import java.time.LocalDateTime;

import tools.vitruv.change.interaction.UserInteractionBase;

public class AsyncTaskStatus {
    private final String taskId;
    private volatile TaskState state;
    private final LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private Object result;
    private String errorMessage;
    private volatile UserInteractionBase pendingInteraction;
    private volatile UserInteractionBase interactionResponse;
    private final Object interactionLock = new Object();

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
        this.state = TaskState.COMPLETED;
        this.result = result;
        this.completedAt = LocalDateTime.now();
    }

    public void fail(String errorMessage) {
        this.state = TaskState.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
    }

    public void waitForUserInteraction() {
        this.state = TaskState.WAITING_USER_INTERACTION;
    }

    public void setWaitingForInteraction(UserInteractionBase interaction) {
        this.pendingInteraction = interaction;
        this.state = TaskState.WAITING_USER_INTERACTION;
    }

    public UserInteractionBase getPendingInteraction() {
        return pendingInteraction;
    }

    @SuppressWarnings("unchecked")
    public <T extends UserInteractionBase> T waitForInteractionResponse() throws InterruptedException {
        synchronized (interactionLock) {
            while (interactionResponse == null && state == TaskState.WAITING_USER_INTERACTION) {
                interactionLock.wait();
            }
            T response = (T) interactionResponse;
            // Cleanup for next interaction
            interactionResponse = null;
            pendingInteraction = null;
            this.state = TaskState.RUNNING;
            return response;
        }
    }

    public void setInteractionResponse(UserInteractionBase response) {
        synchronized (interactionLock) {
            this.interactionResponse = response;
            interactionLock.notifyAll();
        }
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
