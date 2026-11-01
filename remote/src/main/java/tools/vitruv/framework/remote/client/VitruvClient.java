package tools.vitruv.framework.remote.client;

import java.util.List;
import tools.vitruv.change.interaction.UserInteractionBase;
import tools.vitruv.framework.remote.common.rest.TaskStatus;
import tools.vitruv.framework.views.ViewProvider;
import tools.vitruv.framework.views.ViewTypeProvider;

/**
 * A Vitruvius client can remotely access the available {@link
 * tools.vitruv.framework.views.ViewType}s of a Vitruvius instance and query {@link
 * tools.vitruv.framework.views.ViewSelector}s in order to create remotely editable {@link
 * tools.vitruv.framework.views.View}s.
 *
 * <p>Changes to a view can also be propagated asynchronously, see {@link
 * tools.vitruv.framework.remote.client.impl.ChangeRecordingRemoteView#commitChangesAsync()}. The
 * server then answers with a task id right away, and the client follows the propagation with {@link
 * #getTaskStatus(int)} and answers user interactions with {@link #registerUserInteractions(int,
 * List)}.
 */
public interface VitruvClient extends ViewTypeProvider, ViewProvider {
  /**
   * Returns where a change that is propagated asynchronously stands.
   *
   * @param taskId the task id the server assigned to the change
   * @return the status of the change
   * @throws tools.vitruv.framework.remote.client.exception.BadServerResponseException if the server
   *     knows no change with this task id (status code 404) or answered with another error
   */
  TaskStatus getTaskStatus(int taskId);

  /**
   * Sends answers to the user interactions of a change that is propagated asynchronously, e.g., to
   * the question of its {@link TaskStatus} in state {@link
   * tools.vitruv.framework.remote.common.rest.TaskState#WAITING_FOR_INPUT}. The server then goes on
   * with the parts of the change that waited for them.
   *
   * @param taskId the task id the server assigned to the change
   * @param answers the answers, matched to the questions as described at {@link
   *     tools.vitruv.change.interaction.PendingUserInteraction}
   * @return the status of the change after the answers were registered
   * @throws tools.vitruv.framework.remote.client.exception.BadServerResponseException if the server
   *     knows no change with this task id (status code 404), nothing of the change is pending
   *     anymore because it is done or has failed (status code 409), or the server answered with
   *     another error
   */
  TaskStatus registerUserInteractions(int taskId, List<UserInteractionBase> answers);
}
