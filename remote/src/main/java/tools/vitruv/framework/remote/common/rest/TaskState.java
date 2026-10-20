package tools.vitruv.framework.remote.common.rest;

/** The states of a change that is propagated asynchronously, see {@link TaskStatus}. */
public enum TaskState {
  /** The change waits to be propagated or is being propagated right now. */
  QUEUED,

  /**
   * The change needs an answer to a user interaction before it can go on. Its other parts and all
   * other changes are propagated in the meantime.
   */
  WAITING_FOR_INPUT,

  /** The change cannot be propagated right now for another reason than missing user input. */
  BLOCKED,

  /** The change and all changes its propagation produced have been propagated. */
  DONE,

  /**
   * The change could not be propagated. Parts of the change that were queued before the failure are
   * still propagated; while one of them waits for input, the status carries its question, and
   * answers are taken until nothing of the change is pending anymore.
   */
  FAILED
}
