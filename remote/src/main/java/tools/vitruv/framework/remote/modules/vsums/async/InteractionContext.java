package tools.vitruv.framework.remote.modules.vsums.async;

/**
 * Thread-local flags for user-interaction routing during server-side VSUM operations.
 */
public final class InteractionContext {

  private static final ThreadLocal<Boolean> STATE_REPLAY = ThreadLocal.withInitial(() -> false);

  private InteractionContext() {
  }

  public static boolean isStateReplay() {
    return Boolean.TRUE.equals(STATE_REPLAY.get());
  }

  /**
   * Runs an action while replaying a persisted view update (e.g. on VSUM load after restart).
   * User prompts are answered deterministically without blocking HTTP requests.
   */
  public static void runInStateReplay(Runnable action) {
    STATE_REPLAY.set(true);
    try {
      action.run();
    } finally {
      STATE_REPLAY.remove();
    }
  }
}
