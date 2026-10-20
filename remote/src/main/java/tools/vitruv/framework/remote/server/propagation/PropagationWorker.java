package tools.vitruv.framework.remote.server.propagation;

import static com.google.common.base.Preconditions.checkNotNull;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.vitruv.change.atomic.hid.HierarchicalId;
import tools.vitruv.change.atomic.uuid.Uuid;
import tools.vitruv.change.composite.description.VitruviusChange;
import tools.vitruv.change.interaction.PendingUserInteraction;
import tools.vitruv.change.interaction.UserInteractionBase;
import tools.vitruv.change.propagation.NonDeferrablePropagationException;
import tools.vitruv.change.propagation.PropagationBlockReason;
import tools.vitruv.change.propagation.QueuedChange;
import tools.vitruv.change.propagation.QueuedPropagationStep;
import tools.vitruv.change.propagation.QueuedPropagationStep.Outcome;
import tools.vitruv.framework.remote.common.rest.TaskState;
import tools.vitruv.framework.remote.common.rest.TaskStatus;
import tools.vitruv.framework.views.impl.ModifiableView;
import tools.vitruv.framework.views.impl.ViewCreatingViewType;
import tools.vitruv.framework.vsum.VirtualModel;

/**
 * Propagates the changes that clients send for asynchronous propagation, on one background thread
 * per server.
 *
 * <p>{@link #submit} only writes a change down and returns its task id. The worker thread later
 * turns it into a change of the VSUM, enqueues it with {@link
 * VirtualModel#enqueueChange(VitruviusChange)} and propagates the queued changes one step at a
 * time. When a change needs user input, the VSUM sets it aside until the client answers with {@link
 * #registerUserInteractions}, so it does not hold up the other changes.
 *
 * <p>Submitted changes are turned into VSUM changes one at a time, in the order they were
 * submitted: a change is only resolved once the change submitted before it has been applied to the
 * VSUM, which the first propagation step of a change does. So a change recorded after another one
 * in the same view is resolved against models that contain the other one. A change that is blocked
 * for another reason than user input before its first step therefore holds back all changes
 * submitted after it; the VSUM only blocks changes like that when someone asks it to, and this
 * server never does.
 *
 * <p>The worker thread holds the VSUM lock of the server while it resolves a submitted change and
 * while it runs a propagation step, so it never works on the VSUM at the same time as a request.
 * The methods of this class never take that lock, so they return right away, also while a step is
 * running; only {@link #awaitAppliedSubmissions} waits for the worker on purpose.
 */
public class PropagationWorker {
  static final String THREAD_NAME = "vitruv-propagation-worker";

  private static final Logger LOGGER = LoggerFactory.getLogger(PropagationWorker.class);
  private static final Duration DEFAULT_STOP_TIMEOUT = Duration.ofSeconds(10);
  private static final int NO_TASK = 0;

  private final VirtualModel virtualModel;
  private final Object vsumLock;
  private final Duration stopTimeout;

  // Tasks stay in memory for as long as the server runs, so a client can always find out how its
  // change ended. Nothing evicts them yet, which a long-running server will need at some point.
  private final Map<Integer, Task> tasks = new ConcurrentHashMap<>();

  // Submitted changes that have not been applied to the VSUM yet, in the order they were
  // submitted. Only the first one can have been enqueued already. Guarded by its own monitor, which
  // also wakes up whoever waits in awaitAppliedSubmissions.
  private final Deque<Submission> unappliedSubmissions = new ArrayDeque<>();

  // The tasks whose changes are in the VSUM queue, by the task id the VSUM gave their change. Only
  // the worker thread uses it.
  private final Map<Integer, Task> tasksByVsumTaskId = new HashMap<>();

  // Released for every submission and answer. The worker drains it before it looks for work, so
  // work that comes in while it is busy wakes it up again right after.
  private final Semaphore wakeUps = new Semaphore(0);

  private volatile boolean running;
  private Thread thread;

  /** What the worker knows about a submitted change. */
  private static final class Task {
    private final int id;

    // Answers that arrive before the change is enqueued; they are registered right after.
    private final List<UserInteractionBase> earlyAnswers = new ArrayList<>();

    // The task id the VSUM gave the change when it was enqueued, NO_TASK before. The VSUM picks its
    // ids on its own, also for the changes that propagation produces, so they are never handed out
    // to clients. Only set while holding the monitor of the task, so that no early answer gets
    // lost.
    private volatile int vsumTaskId = NO_TASK;

    private volatile String error;

    // Set once nothing of the change is pending anymore, while holding the monitor of the task.
    // The worker sets it before the next step, i.e., before the VSUM can reuse the id of the
    // change, so answers registered under that id never reach another change.
    private volatile boolean finished;

    private Task(int id) {
      this.id = id;
    }
  }

  /** A submitted change that the worker has not applied to the VSUM yet. */
  private record Submission(
      Task task,
      ModifiableView view,
      ViewCreatingViewType<?, HierarchicalId> viewType,
      VitruviusChange<HierarchicalId> change) {}

  /**
   * Creates a worker for the given VSUM. The worker thread is started by {@link #start()}.
   *
   * @param virtualModel the VSUM to propagate the changes in
   * @param vsumLock the lock held by everyone who works on the VSUM or its views
   */
  public PropagationWorker(VirtualModel virtualModel, Object vsumLock) {
    this(virtualModel, vsumLock, DEFAULT_STOP_TIMEOUT);
  }

  /**
   * Creates a worker that waits the given time for a running step when it is stopped before it
   * interrupts the worker thread.
   */
  PropagationWorker(VirtualModel virtualModel, Object vsumLock, Duration stopTimeout) {
    this.virtualModel = checkNotNull(virtualModel, "virtual model must not be null");
    this.vsumLock = checkNotNull(vsumLock, "VSUM lock must not be null");
    this.stopTimeout = checkNotNull(stopTimeout, "stop timeout must not be null");
  }

  /** Starts the worker thread, unless it is running already. */
  public synchronized void start() {
    if (thread != null) {
      return;
    }
    running = true;
    thread = Thread.ofPlatform().name(THREAD_NAME).daemon().start(this::run);
  }

  /**
   * Stops the worker thread and returns once it has ended. A step that is running is allowed to
   * finish first; the thread is interrupted if it has not ended within ten seconds, and stopping
   * still waits for it to end then, since the step may not react to the interrupt. Changes that are
   * still queued stay in the VSUM.
   */
  public synchronized void stop() {
    if (thread == null) {
      return;
    }
    running = false;
    wakeUps.release();
    synchronized (unappliedSubmissions) {
      unappliedSubmissions.notifyAll();
    }
    try {
      thread.join(stopTimeout);
      if (thread.isAlive()) {
        LOGGER.warn("{} did not stop within {}, interrupting it", THREAD_NAME, stopTimeout);
        thread.interrupt();
        thread.join();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    thread = null;
  }

  /**
   * Submits changes made to a view for asynchronous propagation. Returns right away; the worker
   * thread resolves the changes against the VSUM and enqueues them later.
   *
   * @param view the view the changes were made to
   * @param viewType the type of the view, which resolves the changes
   * @param change the changes made to the view
   * @return the task id that identifies the change from now on, a positive number
   */
  public int submit(
      ModifiableView view,
      ViewCreatingViewType<?, HierarchicalId> viewType,
      VitruviusChange<HierarchicalId> change) {
    Task task;
    do {
      task = new Task(ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE));
    } while (tasks.putIfAbsent(task.id, task) != null);
    synchronized (unappliedSubmissions) {
      unappliedSubmissions.addLast(new Submission(task, view, viewType, change));
    }
    wakeUps.release();
    return task.id;
  }

  /**
   * Waits until every change submitted for the given view has been applied to the VSUM, or has
   * failed before. Changes made to the view afterwards can then be resolved against models that
   * contain the submitted ones, e.g., to propagate them right away.
   *
   * @param view the view whose submitted changes to wait for
   * @throws InterruptedException if the calling thread is interrupted while waiting
   * @throws IllegalStateException if changes of the view are still waiting but the worker is not
   *     running, so they would not be applied
   */
  public void awaitAppliedSubmissions(ModifiableView view) throws InterruptedException {
    synchronized (unappliedSubmissions) {
      while (unappliedSubmissions.stream().anyMatch(it -> it.view() == view)) {
        if (!running) {
          throw new IllegalStateException(
              "Changes of the view sent for asynchronous propagation have not been applied yet,"
                  + " and the propagation worker is not running");
        }
        unappliedSubmissions.wait();
      }
    }
  }

  /**
   * Returns where the change submitted under the given task id stands.
   *
   * @param taskId the id returned by {@link #submit}
   * @return the status, empty if no change was submitted under this id
   */
  public Optional<TaskStatus> getStatus(int taskId) {
    return Optional.ofNullable(tasks.get(taskId)).map(this::statusOf);
  }

  /**
   * Registers answers to user interactions of the change submitted under the given task id, so the
   * parts of the change that wait for them are propagated again. Answers that arrive before the
   * change is enqueued are kept until it is. Answers are taken as long as anything of the change is
   * pending, also after a part of it failed.
   *
   * @param taskId the id returned by {@link #submit}
   * @param answers the answers, matched to the questions as described at {@link
   *     PendingUserInteraction}
   * @throws NoSuchElementException if no change was submitted under this id
   * @throws IllegalStateException if nothing of the change is pending anymore, i.e., it has been
   *     propagated completely or failed
   */
  public void registerUserInteractions(int taskId, List<UserInteractionBase> answers) {
    Task task = tasks.get(taskId);
    if (task == null) {
      throw new NoSuchElementException("No change was submitted with task id " + taskId);
    }
    synchronized (task) {
      if (task.finished) {
        throw alreadyFinished(taskId);
      }
      if (task.vsumTaskId == NO_TASK) {
        task.earlyAnswers.addAll(answers);
        return;
      }
      try {
        virtualModel.registerUserInteractions(
            task.vsumTaskId, answers.toArray(UserInteractionBase[]::new));
      } catch (IllegalArgumentException e) {
        // The last part of the change was propagated, and the worker has not marked the task as
        // finished yet. It cannot do so while this monitor is held, so nothing else has the id yet.
        throw alreadyFinished(taskId);
      }
    }
    wakeUps.release();
  }

  private void run() {
    while (running) {
      try {
        Submission firstUnapplied = firstUnappliedSubmission();
        if (firstUnapplied != null && firstUnapplied.task().vsumTaskId == NO_TASK) {
          enqueue(firstUnapplied);
          continue;
        }
        Optional<QueuedChange> next = nextChangeToPropagate();
        if (next.isPresent()) {
          propagateNextStep(next.get());
        } else {
          wakeUps.acquire();
          wakeUps.drainPermits();
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      } catch (RuntimeException e) {
        // If the worker died, no change would be propagated anymore, so keep going.
        LOGGER.error("Unexpected error in the propagation worker", e);
      }
    }
  }

  private Submission firstUnappliedSubmission() {
    synchronized (unappliedSubmissions) {
      return unappliedSubmissions.peekFirst();
    }
  }

  private void enqueue(Submission submission) {
    Task task = submission.task();
    int vsumTaskId;
    try {
      synchronized (vsumLock) {
        VitruviusChange<Uuid> change =
            submission.viewType().resolveViewChanges(submission.view(), submission.change());
        vsumTaskId = virtualModel.enqueueChange(change);
      }
    } catch (RuntimeException e) {
      fail(task, messageOf(e), e);
      finish(task);
      markApplied(submission);
      return;
    }
    tasksByVsumTaskId.put(vsumTaskId, task);
    synchronized (task) {
      task.vsumTaskId = vsumTaskId;
      if (!task.earlyAnswers.isEmpty()) {
        virtualModel.registerUserInteractions(
            vsumTaskId, task.earlyAnswers.toArray(UserInteractionBase[]::new));
        task.earlyAnswers.clear();
      }
    }
  }

  /**
   * Returns the change that the next step will take, if any queued change can be propagated. Only
   * this worker takes steps, and changes are only appended to the queue, so that is the first
   * queued change.
   */
  private Optional<QueuedChange> nextChangeToPropagate() {
    List<QueuedChange> queued = virtualModel.getQueuedChanges();
    if (queued.stream().allMatch(QueuedChange::blocked)) {
      return Optional.empty();
    }
    return Optional.of(queued.get(0));
  }

  private void propagateNextStep(QueuedChange next) {
    boolean taken;
    try {
      Optional<QueuedPropagationStep> step;
      synchronized (vsumLock) {
        step = virtualModel.propagateNextQueuedChange();
      }
      taken = step.isPresent() && step.get().outcome() != Outcome.SKIPPED_BLOCKED;
    } catch (NonDeferrablePropagationException e) {
      taken = true;
      fail(
          next.rootTaskId(),
          "The change needs user input, but its propagation had already modified models when it"
              + " asked for it, so it cannot wait for the answer. Send the answers along with the"
              + " change instead.",
          e);
    } catch (RuntimeException e) {
      taken = true;
      fail(next.rootTaskId(), messageOf(e), e);
    }
    Submission firstUnapplied = firstUnappliedSubmission();
    // The first step that takes a change from the queue applies it to the models, or drops it if
    // the step fails. Either way, the change submitted next can be resolved now.
    if (taken && firstUnapplied != null && firstUnapplied.task().vsumTaskId == next.taskId()) {
      markApplied(firstUnapplied);
    }
    finishTasksWithoutPendingParts();
  }

  private void markApplied(Submission submission) {
    synchronized (unappliedSubmissions) {
      unappliedSubmissions.remove(submission);
      unappliedSubmissions.notifyAll();
    }
  }

  /** Marks every task as finished of which nothing is in the VSUM queue anymore. */
  private void finishTasksWithoutPendingParts() {
    if (tasksByVsumTaskId.isEmpty()) {
      return;
    }
    Set<Integer> pendingRoots =
        virtualModel.getQueuedChanges().stream()
            .map(QueuedChange::rootTaskId)
            .collect(Collectors.toSet());
    tasksByVsumTaskId
        .entrySet()
        .removeIf(
            entry -> {
              if (pendingRoots.contains(entry.getKey())) {
                return false;
              }
              finish(entry.getValue());
              return true;
            });
  }

  private static void finish(Task task) {
    synchronized (task) {
      task.finished = true;
    }
  }

  private void fail(int vsumTaskId, String error, RuntimeException cause) {
    Task task = tasksByVsumTaskId.get(vsumTaskId);
    if (task != null) {
      fail(task, error, cause);
    } else {
      // A change that someone other than this worker enqueued has no task here.
      LOGGER.warn("Propagation of the change with VSUM task id {} failed: {}", vsumTaskId, error);
    }
  }

  private static void fail(Task task, String error, RuntimeException cause) {
    LOGGER.warn("Propagation of the change with task id {} failed: {}", task.id, error, cause);
    // Other parts of the change are still propagated and may fail as well; the first error is the
    // one to report.
    if (task.error == null) {
      task.error = error;
    }
  }

  private TaskStatus statusOf(Task task) {
    if (task.finished) {
      String error = task.error;
      return error != null
          ? new TaskStatus(task.id, TaskState.FAILED, null, error)
          : new TaskStatus(task.id, TaskState.DONE, null, null);
    }
    int vsumTaskId = task.vsumTaskId;
    List<QueuedChange> queuedParts =
        vsumTaskId == NO_TASK
            ? List.of()
            : virtualModel.getQueuedChanges().stream()
                .filter(it -> it.rootTaskId() == vsumTaskId)
                .toList();
    PendingUserInteraction question =
        queuedParts.stream()
            .filter(it -> it.blockReason() == PropagationBlockReason.USER_INTERACTION)
            .map(QueuedChange::pendingInteraction)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    String error = task.error;
    if (error != null) {
      // Parts of the change that were queued before the failure are still propagated, and their
      // questions still have to be answered.
      return new TaskStatus(task.id, TaskState.FAILED, question, error);
    }
    if (question != null) {
      return new TaskStatus(task.id, TaskState.WAITING_FOR_INPUT, question, null);
    }
    if (queuedParts.stream().anyMatch(QueuedChange::blocked)) {
      return new TaskStatus(task.id, TaskState.BLOCKED, null, null);
    }
    // Also while a step runs or before the worker has marked a finished change as such.
    return new TaskStatus(task.id, TaskState.QUEUED, null, null);
  }

  private static IllegalStateException alreadyFinished(int taskId) {
    return new IllegalStateException(
        "The change with task id " + taskId + " has already been propagated or has failed");
  }

  private static String messageOf(RuntimeException exception) {
    return exception.getMessage() != null ? exception.getMessage() : exception.getClass().getName();
  }
}
