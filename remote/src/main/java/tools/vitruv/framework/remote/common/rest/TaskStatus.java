package tools.vitruv.framework.remote.common.rest;

import tools.vitruv.change.interaction.PendingUserInteraction;

/**
 * Where a change that is propagated asynchronously stands. The server reports it when it accepts
 * the change and whenever a client asks for it.
 *
 * @param taskId the id the server assigned to the change
 * @param state the state of the change
 * @param question the user interaction the change waits for if the state is {@link
 *     TaskState#WAITING_FOR_INPUT}, {@code null} otherwise. A change has at most one open question
 *     at a time: its parts are propagated in order, so the parts after the one that asked wait for
 *     the answer before they can ask anything themselves.
 * @param error why the propagation failed if the state is {@link TaskState#FAILED}, {@code null}
 *     otherwise
 */
public record TaskStatus(
    int taskId, TaskState state, PendingUserInteraction question, String error) {}
