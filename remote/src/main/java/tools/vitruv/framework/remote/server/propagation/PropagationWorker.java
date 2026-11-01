package tools.vitruv.framework.remote.server.propagation;

import static com.google.common.base.Preconditions.checkNotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
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
 * #registerUserInteractions}, so it does not hold up the changes of other views.
 *
 * <p>The changes of one view are propagated in the order they were submitted: a change is only
 * resolved and enqueued once every change submitted before it for the same view has been propagated
 * completely or has failed. So it is resolved against models that contain those changes, and the
 * reactions to it run after theirs and find what they created, such as correspondences. While a
 * change waits for user input, or is blocked for another reason, the changes submitted after it for
 * the same view therefore wait as well. Changes of different views do not wait for each other; of
 * the changes that may be resolved, the one submitted first is resolved first.
 *
 * <p>The worker thread holds the VSUM lock of the server while it resolves a submitted change and
 * while it runs a propagation step, so it never works on the VSUM at the same time as a request.
 * The methods of this class never take that lock, so they return right away, also while a step is
 * running; only {@link #awaitFinishedSubmissions} waits for the worker on purpose.
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

  // Submitted changes that have neither been propagated completely nor failed, in the order they
  // were submitted. Of the changes of one view, only the first one can have been enqueued already.
  // Guarded by its own monitor, which also wakes up whoever waits in awaitFinishedSubmissions.
  private final List<Submission> unfinishedSubmissions = new ArrayList<>();

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

  /** A submitted change that has neither been propagated completely nor failed yet. */
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
    synchronized (unfinishedSubmissions) {
      unfinishedSubmissions.notifyAll();
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
   * thread resolves the changes against the VSUM and enqueues them later, once the changes
   * submitted for the view before have been propagated completely or have failed.
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
    synchronized (unfinishedSubmissions) {
      unfinishedSubmissions.add(new Submission(task, view, viewType, change));
    }
    wakeUps.release();
    return task.id;
  }

  /**
   * Waits until every change submitted for the given view has been propagated completely or has
   * failed, so that a change made to the view afterwards can be propagated right away, in order
   * after them. A change that waits for user input may wait for a person for an arbitrarily long
   * time, though, so this method does not wait for one: it throws as soon as one of the changes of
   * the view waits for input, also if it only starts to while this method waits.
   *
   * @param view the view whose submitted changes to wait for
   * @throws WaitingForInputException if a change submitted for the view waits for user input
   * @throws InterruptedException if the calling thread is interrupted while waiting
   * @throws IllegalStateException if changes of the view are still pending but the worker is not
   *     running, so they would not be propagated
   */
  public void awaitFinishedSubmissions(ModifiableView view)
      throws WaitingForInputException, InterruptedException {
    synchronized (unfinishedSubmissions) {
      List<Task> pending = unfinishedTasksOf(view);
      while (!pending.isEmpty()) {
        Optional<Task> waitingForInput =
            pending.stream()
                .filter(it -> statusOf(it).state() == TaskState.WAITING_FOR_INPUT)
                .findFirst();
        if (waitingForInput.isPresent()) {
          throw new WaitingForInputException(waitingForInput.get().id);
        }
        if (!running) {
          throw new IllegalStateException(
              "Changes of the view sent for asynchronous propagation have not been propagated yet,"
                  + " and the propagation worker is not running");
        }
        unfinishedSubmissions.wait();
        pending = unfinishedTasksOf(view);
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
   * part of the change that waits for them is propagated again. Answers that arrive before the
   * change is enqueued are kept until it is. Answers are taken as long as anything of the change is
   * pending.
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
        // The change has been propagated completely or has failed, and the worker has not marked
        // the task as finished yet. It cannot do so while this monitor is held, so nothing else has
        // the id yet.
        throw alreadyFinished(taskId);
      }
    }
    wakeUps.release();
  }

  private void run() {
    while (running) {
      try {
        Submission resolvable = nextSubmissionToResolve();
        if (resolvable != null) {
          enqueue(resolvable);
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

  /**
   * Returns the first submitted change that has not been enqueued yet and that no unfinished change
   * submitted before it for the same view holds back, {@code null} if there is none.
   */
  private Submission nextSubmissionToResolve() {
    synchronized (unfinishedSubmissions) {
      Set<ModifiableView> viewsWithEarlierChanges =
          Collections.newSetFromMap(new IdentityHashMap<>());
      for (Submission submission : unfinishedSubmissions) {
        if (viewsWithEarlierChanges.add(submission.view())
            && submission.task().vsumTaskId == NO_TASK) {
          return submission;
        }
      }
      return null;
    }
  }

  private List<Task> unfinishedTasksOf(ModifiableView view) {
    return unfinishedSubmissions.stream()
        .filter(it -> it.view() == view)
        .map(Submission::task)
        .toList();
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
      removeFinishedSubmissions();
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
   * this worker takes steps, and the VSUM only reorders its queue within a step, while enqueuing
   * appends to it, so that is the first queued change.
   */
  private Optional<QueuedChange> nextChangeToPropagate() {
    List<QueuedChange> queued = virtualModel.getQueuedChanges();
    if (queued.stream().allMatch(QueuedChange::blocked)) {
      return Optional.empty();
    }
    return Optional.of(queued.get(0));
  }

  private void propagateNextStep(QueuedChange next) {
    try {
      synchronized (vsumLock) {
        virtualModel.propagateNextQueuedChange();
      }
    } catch (NonDeferrablePropagationException e) {
      fail(
          next.rootTaskId(),
          "The change needs user input, but its propagation had already modified models when it"
              + " asked for it, so it cannot wait for the answer. Send the answers along with the"
              + " change instead.",
          e);
    } catch (RuntimeException e) {
      if (virtualModel.isChangePending(next.rootTaskId())) {
        // A failed step drops the whole change, so the step went through, and saving the models or
        // notifying a listener failed afterwards. The rest of the change stays queued and goes on;
        // failing the task now would let the next change of the view overtake it.
        LOGGER.error(
            "Finishing a propagation step of the change with VSUM task id {} failed; the rest of"
                + " the change stays queued",
            next.rootTaskId(),
            e);
      } else {
        fail(next.rootTaskId(), messageOf(e), e);
      }
    }
    finishTasksWithoutPendingParts();
    removeFinishedSubmissions();
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

  /**
   * Forgets the submitted changes that have been propagated completely or have failed, so the
   * changes submitted after them for the same view can be resolved, and wakes up whoever waits in
   * {@link #awaitFinishedSubmissions}. They are woken up even if no change finished, since a change
   * may have started to wait for user input.
   */
  private void removeFinishedSubmissions() {
    synchronized (unfinishedSubmissions) {
      unfinishedSubmissions.removeIf(it -> it.task().finished);
      unfinishedSubmissions.notifyAll();
    }
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
    task.error = error;
  }

  private TaskStatus statusOf(Task task) {
    // A task only fails once nothing of its change is queued anymore, so a failed change has no
    // question left. A failed task gets its error before it is marked finished, so reading finished
    // first ensures that a finished task without an error is done.
    boolean finished = task.finished;
    String error = task.error;
    if (error != null) {
      return new TaskStatus(task.id, TaskState.FAILED, null, error);
    }
    if (finished) {
      return new TaskStatus(task.id, TaskState.DONE, null, null);
    }
    int vsumTaskId = task.vsumTaskId;
    List<QueuedChange> queuedParts =
        vsumTaskId == NO_TASK
            ? List.of()
            : virtualModel.getQueuedChanges().stream()
                .filter(it -> it.rootTaskId() == vsumTaskId)
                .toList();
    // The parts of a change are propagated in order, so at most one of them waits for an answer.
    // The parts after it wait for it with the same block reason, but without a question.
    PendingUserInteraction question =
        queuedParts.stream()
            .filter(it -> it.blockReason() == PropagationBlockReason.USER_INTERACTION)
            .map(QueuedChange::pendingInteraction)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    if (question != null) {
      return new TaskStatus(task.id, TaskState.WAITING_FOR_INPUT, question, null);
    }
    if (queuedParts.stream().anyMatch(QueuedChange::blocked)) {
      return new TaskStatus(task.id, TaskState.BLOCKED, null, null);
    }
    // Also while the change waits for the changes submitted before it for the same view, while a
    // step runs, or before the worker has marked a finished change as such.
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
