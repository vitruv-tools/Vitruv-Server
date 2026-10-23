package tools.vitruv.framework.remote.common.rest;

/** The states of a change that is propagated asynchronously, see {@link TaskStatus}. */
public enum TaskState {
  /**
   * The change waits to be propagated or is being propagated right now. It stays queued at least
   * until the changes sent from the same view before it have been propagated completely or have
   * failed.
   */
  QUEUED,

  /**
   * The change needs an answer to a user interaction before it can go on. The rest of the change
   * and the changes sent from the same view after it wait as well, while the changes of other views
   * are propagated in the meantime.
   */
  WAITING_FOR_INPUT,

  /** The change cannot be propagated right now for another reason than missing user input. */
  BLOCKED,

  /** The change and all changes its propagation produced have been propagated. */
  DONE,

  /**
   * The change could not be propagated. As with a synchronous propagation, nothing more of it is
   * propagated after the failure, and the models keep what was propagated before. Answers for it
   * are rejected.
   */
  FAILED
}
