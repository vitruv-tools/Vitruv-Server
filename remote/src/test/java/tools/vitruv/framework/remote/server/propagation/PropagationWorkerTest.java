package tools.vitruv.framework.remote.server.propagation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.Uninterruptibles;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
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
 * Tests the {@link PropagationWorker} with its real thread against a mocked VSUM whose queue is
 * simulated by this test.
 */
class PropagationWorkerTest {
  private static final Duration TIMEOUT = Duration.ofSeconds(10);
  private static final PendingUserInteraction QUESTION =
      new PendingUserInteraction(
          PendingUserInteraction.Kind.SINGLE_SELECTION,
          "Pick one",
          "Which class should be created?",
          "OK",
          null,
          "Cancel",
          List.of("Component", "Interface"));

  private final Object vsumLock = new Object();
  private final VirtualModel vsum = mock(VirtualModel.class);
  private final ModifiableView view = mock(ModifiableView.class);

  @SuppressWarnings("unchecked")
  private final ViewCreatingViewType<?, HierarchicalId> viewType = mock(ViewCreatingViewType.class);

  @SuppressWarnings("unchecked")
  private final VitruviusChange<HierarchicalId> viewChange = mock(VitruviusChange.class);

  @SuppressWarnings("unchecked")
  private final VitruviusChange<Uuid> resolvedChange = mock(VitruviusChange.class);

  private final UserInteractionBase answer = mock(UserInteractionBase.class);

  // The queue of the simulated VSUM; all access is synchronized on it.
  private final List<QueuedChange> queue = new ArrayList<>();
  private final Map<Integer, List<UserInteractionBase>> answersByRoot = new ConcurrentHashMap<>();
  private final List<Boolean> lockHeldInVsum = new CopyOnWriteArrayList<>();

  // The simulated VSUM picks its own task ids, far away from anything a test uses otherwise.
  private final AtomicInteger nextVsumTaskId = new AtomicInteger(1_000_000);
  private final List<Integer> vsumTaskIds = new CopyOnWriteArrayList<>();

  // What the simulated VSUM was asked to do, in order.
  private final List<String> events = new CopyOnWriteArrayList<>();

  // What a propagation step of the simulated VSUM does with the first queued change, which has
  // already been taken off the queue. By default, it is propagated completely.
  private volatile FakeStep step = PropagationWorkerTest::propagated;

  private PropagationWorker worker;

  @FunctionalInterface
  private interface FakeStep {
    QueuedPropagationStep take(QueuedChange first);
  }

  @BeforeEach
  void simulateVsumQueue() {
    when(viewType.resolveViewChanges(any(), any()))
        .thenAnswer(
            invocation -> {
              lockHeldInVsum.add(Thread.holdsLock(vsumLock));
              events.add("resolve " + invocation.getArgument(1));
              return resolvedChange;
            });
    when(vsum.enqueueChange(any()))
        .thenAnswer(
            invocation -> {
              int taskId = nextVsumTaskId.getAndAdd(1000);
              synchronized (queue) {
                queue.add(new QueuedChange(taskId, null, taskId, null, null));
              }
              vsumTaskIds.add(taskId);
              events.add("enqueue " + taskId);
              return taskId;
            });
    when(vsum.getQueuedChanges())
        .thenAnswer(
            invocation -> {
              synchronized (queue) {
                return List.copyOf(queue);
              }
            });
    when(vsum.isChangePending(anyInt()))
        .thenAnswer(invocation -> isPending(invocation.getArgument(0)));
    doAnswer(
            invocation -> {
              int taskId = invocation.getArgument(0);
              UserInteractionBase[] answers =
                  (UserInteractionBase[]) invocation.getRawArguments()[1];
              synchronized (queue) {
                if (!isPending(taskId)) {
                  throw new IllegalArgumentException("No pending change with task id " + taskId);
                }
                answersByRoot
                    .computeIfAbsent(taskId, id -> new ArrayList<>())
                    .addAll(List.of(answers));
                queue.replaceAll(
                    it ->
                        it.rootTaskId() == taskId && it.blocked()
                            ? new QueuedChange(it.taskId(), it.parentTaskId(), taskId, null, null)
                            : it);
              }
              return null;
            })
        .when(vsum)
        .registerUserInteractions(anyInt(), any(UserInteractionBase[].class));
    when(vsum.propagateNextQueuedChange())
        .thenAnswer(
            invocation -> {
              lockHeldInVsum.add(Thread.holdsLock(vsumLock));
              QueuedChange first;
              synchronized (queue) {
                if (queue.isEmpty()) {
                  return Optional.empty();
                }
                first = queue.remove(0);
                if (first.blocked()) {
                  queue.add(first);
                  return Optional.of(
                      new QueuedPropagationStep(
                          first.taskId(), Outcome.SKIPPED_BLOCKED, List.of()));
                }
              }
              events.add("step " + first.taskId());
              return Optional.of(step.take(first));
            });
    worker = new PropagationWorker(vsum, vsumLock);
  }

  @AfterEach
  void stopWorker() {
    worker.stop();
  }

  @Test
  void submit_resolvesAndEnqueuesTheChange() {
    worker.start();

    int taskId = worker.submit(view, viewType, viewChange);

    assertTrue(taskId > 0, "task ids are positive");
    verify(vsum, timeout(TIMEOUT.toMillis())).enqueueChange(resolvedChange);
    awaitState(taskId, TaskState.DONE);
  }

  @Test
  void resolvingAndPropagating_holdTheVsumLock() {
    worker.start();

    awaitState(worker.submit(view, viewType, viewChange), TaskState.DONE);

    assertEquals(List.of(true, true), lockHeldInVsum, "one resolve and one propagation step");
  }

  @Test
  void submissions_areResolvedOneAtATimeOnceTheChangeBeforeHasBeenApplied() {
    // Queued by someone else, so the first submitted change is not the next one to propagate.
    enqueueForeignChange(42);
    VitruviusChange<HierarchicalId> laterViewChange = mockViewChange();
    // Both submissions are there when the worker starts, as when it is busy while they come in.
    int first = worker.submit(view, viewType, viewChange);
    int second = worker.submit(view, viewType, laterViewChange);

    worker.start();
    awaitState(first, TaskState.DONE);
    awaitState(second, TaskState.DONE);

    // The first change is applied when its first step takes it, and only then is the second one
    // resolved, against models that contain the first one.
    assertEquals(
        List.of(
            "resolve " + viewChange,
            "enqueue " + vsumTaskIds.get(0),
            "step 42",
            "step " + vsumTaskIds.get(0),
            "resolve " + laterViewChange,
            "enqueue " + vsumTaskIds.get(1),
            "step " + vsumTaskIds.get(1)),
        events);
  }

  @Test
  void changeWaitingForInput_doesNotHoldUpTheSubmissionsAfterIt() {
    step = this::askUnlessAnswered;
    int first = worker.submit(view, viewType, viewChange);
    int second = worker.submit(view, viewType, mockViewChange());

    worker.start();

    // A deferred step has applied the change already, so the next one can be resolved.
    awaitState(first, TaskState.WAITING_FOR_INPUT);
    awaitState(second, TaskState.WAITING_FOR_INPUT);
  }

  @Test
  void status_isQueuedUntilTheChangeIsEnqueued() throws InterruptedException {
    CountDownLatch resolving = new CountDownLatch(1);
    CountDownLatch resolve = new CountDownLatch(1);
    doAnswer(
            invocation -> {
              resolving.countDown();
              assertTrue(resolve.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
              return resolvedChange;
            })
        .when(viewType)
        .resolveViewChanges(view, viewChange);
    worker.start();

    int taskId = worker.submit(view, viewType, viewChange);
    assertTrue(resolving.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));

    assertEquals(TaskState.QUEUED, worker.getStatus(taskId).orElseThrow().state());
    verify(vsum, never()).enqueueChange(any());
    resolve.countDown();
    awaitState(taskId, TaskState.DONE);
  }

  @Test
  void status_isWaitingForInputWithTheQuestion() {
    step = this::askUnlessAnswered;
    worker.start();

    int taskId = worker.submit(view, viewType, viewChange);

    TaskStatus status = awaitState(taskId, TaskState.WAITING_FOR_INPUT);
    assertEquals(new TaskStatus(taskId, TaskState.WAITING_FOR_INPUT, QUESTION, null), status);
  }

  @Test
  void answersAfterEnqueuing_areRegisteredAndTheChangeGoesOn() {
    step = this::askUnlessAnswered;
    worker.start();
    int taskId = worker.submit(view, viewType, viewChange);
    awaitState(taskId, TaskState.WAITING_FOR_INPUT);

    worker.registerUserInteractions(taskId, List.of(answer));

    verify(vsum).registerUserInteractions(vsumTaskIds.get(0), answer);
    awaitState(taskId, TaskState.DONE);
  }

  @Test
  void answersBeforeEnqueuing_areRegisteredRightAfterTheChangeIsEnqueued()
      throws InterruptedException {
    CountDownLatch resolve = new CountDownLatch(1);
    doAnswer(
            invocation -> {
              assertTrue(resolve.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
              return resolvedChange;
            })
        .when(viewType)
        .resolveViewChanges(view, viewChange);
    step = this::askUnlessAnswered;
    worker.start();
    int taskId = worker.submit(view, viewType, viewChange);

    worker.registerUserInteractions(taskId, List.of(answer));
    verify(vsum, never()).registerUserInteractions(anyInt(), any(UserInteractionBase[].class));
    resolve.countDown();

    awaitState(taskId, TaskState.DONE);
    InOrder inOrder = inOrder(vsum);
    inOrder.verify(vsum).enqueueChange(resolvedChange);
    inOrder.verify(vsum).registerUserInteractions(vsumTaskIds.get(0), answer);
    inOrder.verify(vsum).propagateNextQueuedChange();
  }

  @Test
  void failingStep_marksItsRootTaskFailedAndTheWorkerGoesOn() {
    AtomicBoolean failedOnce = new AtomicBoolean();
    step =
        first -> {
          if (first.taskId() == first.rootTaskId()) {
            // The change produces a follow-up change, which is propagated next.
            addFollowUps(first, first.taskId() + 1);
            return propagated(first);
          }
          if (failedOnce.compareAndSet(false, true)) {
            throw new IllegalStateException("reaction failed");
          }
          return propagated(first);
        };
    worker.start();

    int failing = worker.submit(view, viewType, viewChange);
    TaskStatus status = awaitState(failing, TaskState.FAILED);
    int succeeding = worker.submit(view, viewType, viewChange);

    assertEquals(new TaskStatus(failing, TaskState.FAILED, null, "reaction failed"), status);
    awaitState(succeeding, TaskState.DONE);
  }

  @Test
  void failedTask_reportsTheQuestionOfAPartStillPendingAndTakesAnswersForIt() {
    step =
        first -> {
          int root = first.rootTaskId();
          if (first.taskId() == root) {
            // Two specifications react to the change.
            addFollowUps(first, root + 1, root + 2);
            return propagated(first);
          }
          if (first.taskId() == root + 1) {
            throw new IllegalStateException("reaction failed");
          }
          return askUnlessAnswered(first);
        };
    worker.start();
    int taskId = worker.submit(view, viewType, viewChange);

    // The second follow-up is still propagated and asks for input after the first one failed.
    TaskStatus failedAndWaiting =
        awaitStatus(taskId, status -> status.question() != null, "a question");
    assertEquals(
        new TaskStatus(taskId, TaskState.FAILED, QUESTION, "reaction failed"), failedAndWaiting);

    worker.registerUserInteractions(taskId, List.of(answer));

    verify(vsum).registerUserInteractions(vsumTaskIds.get(0), answer);
    awaitNothingQueued();
    assertEquals(
        new TaskStatus(taskId, TaskState.FAILED, null, "reaction failed"),
        worker.getStatus(taskId).orElseThrow());
    assertThrows(
        IllegalStateException.class, () -> worker.registerUserInteractions(taskId, List.of()));
  }

  @Test
  void status_isNotDoneWhileAFailingStepIsStillRunning() throws InterruptedException {
    CountDownLatch stepRunning = new CountDownLatch(1);
    CountDownLatch failStep = new CountDownLatch(1);
    step =
        first -> {
          // The change is already off the queue here, like in a real step.
          stepRunning.countDown();
          try {
            assertTrue(failStep.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
          throw new IllegalStateException("reaction failed");
        };
    worker.start();
    int taskId = worker.submit(view, viewType, viewChange);
    assertTrue(stepRunning.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));

    assertEquals(TaskState.QUEUED, worker.getStatus(taskId).orElseThrow().state());
    failStep.countDown();
    awaitState(taskId, TaskState.FAILED);
  }

  @Test
  void nonDeferrableStep_failsWithAHintToSendTheAnswersAlong() {
    step =
        first -> {
          throw new NonDeferrablePropagationException(
              "models were modified", new IllegalStateException("pending"));
        };
    worker.start();

    TaskStatus status = awaitState(worker.submit(view, viewType, viewChange), TaskState.FAILED);

    assertTrue(
        status.error().contains("Send the answers along with the change"),
        "unexpected error: " + status.error());
  }

  @Test
  void failingResolve_marksTheTaskFailedWithoutEnqueuing() {
    doThrow(new UnsupportedOperationException("cannot resolve view changes"))
        .when(viewType)
        .resolveViewChanges(view, viewChange);
    worker.start();

    int taskId = worker.submit(view, viewType, viewChange);

    assertEquals(
        new TaskStatus(taskId, TaskState.FAILED, null, "cannot resolve view changes"),
        awaitState(taskId, TaskState.FAILED));
    verify(vsum, never()).enqueueChange(any());
  }

  @Test
  void unknownTask_hasNoStatusAndTakesNoAnswers() {
    worker.start();

    assertEquals(Optional.empty(), worker.getStatus(4711));
    assertThrows(
        NoSuchElementException.class, () -> worker.registerUserInteractions(4711, List.of(answer)));
  }

  @Test
  void finishedTasks_takeNoAnswers() {
    doReturn(resolvedChange)
        .doThrow(new UnsupportedOperationException("cannot resolve view changes"))
        .when(viewType)
        .resolveViewChanges(view, viewChange);
    worker.start();
    int done = worker.submit(view, viewType, viewChange);
    awaitState(done, TaskState.DONE);
    int failed = worker.submit(view, viewType, viewChange);
    awaitState(failed, TaskState.FAILED);

    assertThrows(
        IllegalStateException.class, () -> worker.registerUserInteractions(done, List.of(answer)));
    assertThrows(
        IllegalStateException.class,
        () -> worker.registerUserInteractions(failed, List.of(answer)));
  }

  @Test
  void answersForAFinishedTask_neverReachAChangeTheVsumGaveTheSameIdLater() {
    worker.start();
    int done = worker.submit(view, viewType, viewChange);
    awaitState(done, TaskState.DONE);
    // The VSUM picks ids for follow-up changes on its own and knows nothing about the ids of the
    // worker, so it may well pick the id of the finished task.
    step =
        first -> {
          if (first.taskId() == first.rootTaskId()) {
            addFollowUps(first, done);
            return propagated(first);
          }
          return askUnlessAnswered(first);
        };
    int waiting = worker.submit(view, viewType, viewChange);
    awaitState(waiting, TaskState.WAITING_FOR_INPUT);

    assertThrows(
        IllegalStateException.class, () -> worker.registerUserInteractions(done, List.of(answer)));

    verify(vsum, never()).registerUserInteractions(anyInt(), any(UserInteractionBase[].class));
    assertEquals(TaskState.WAITING_FOR_INPUT, worker.getStatus(waiting).orElseThrow().state());
  }

  @Test
  void submittingAskingAndAnswering_doNotWaitForTheVsumLock() throws InterruptedException {
    step = this::askUnlessAnswered;
    worker.start();
    int waiting = worker.submit(view, viewType, viewChange);
    awaitState(waiting, TaskState.WAITING_FOR_INPUT);
    CountDownLatch lockTaken = new CountDownLatch(1);
    CountDownLatch releaseLock = new CountDownLatch(1);
    Thread lockHolder =
        Thread.ofPlatform()
            .start(
                () -> {
                  synchronized (vsumLock) {
                    lockTaken.countDown();
                    try {
                      releaseLock.await();
                    } catch (InterruptedException e) {
                      Thread.currentThread().interrupt();
                    }
                  }
                });
    assertTrue(lockTaken.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));

    int submitted;
    try {
      submitted =
          assertTimeoutPreemptively(
              TIMEOUT,
              () -> {
                int taskId = worker.submit(view, viewType, viewChange);
                assertEquals(TaskState.QUEUED, worker.getStatus(taskId).orElseThrow().state());
                assertEquals(
                    TaskState.WAITING_FOR_INPUT, worker.getStatus(waiting).orElseThrow().state());
                worker.registerUserInteractions(waiting, List.of(answer));
                return taskId;
              });
    } finally {
      releaseLock.countDown();
      lockHolder.join();
    }

    // Once the lock is free again, the worker catches up with both changes.
    awaitState(waiting, TaskState.DONE);
    awaitState(submitted, TaskState.WAITING_FOR_INPUT);
  }

  @Test
  void awaitAppliedSubmissions_waitsUntilTheChangesOfTheViewHaveBeenApplied() throws Exception {
    CountDownLatch resolve = new CountDownLatch(1);
    doAnswer(
            invocation -> {
              assertTrue(resolve.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
              return resolvedChange;
            })
        .when(viewType)
        .resolveViewChanges(view, viewChange);
    worker.start();
    worker.submit(view, viewType, viewChange);
    CountDownLatch applied = new CountDownLatch(1);
    Thread.ofPlatform()
        .start(
            () -> {
              try {
                worker.awaitAppliedSubmissions(view);
                events.add("applied");
                applied.countDown();
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
              }
            });

    // Other views have nothing to wait for.
    assertTimeoutPreemptively(
        TIMEOUT, () -> worker.awaitAppliedSubmissions(mock(ModifiableView.class)));
    assertFalse(applied.await(200, TimeUnit.MILLISECONDS), "the change is not even resolved yet");
    resolve.countDown();

    assertTrue(applied.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
    assertTrue(
        events.indexOf("step " + vsumTaskIds.get(0)) < events.indexOf("applied"),
        "unexpected order: " + events);
  }

  @Test
  void awaitAppliedSubmissions_doesNotWaitForAChangeThatCouldNotBeResolved() {
    doThrow(new UnsupportedOperationException("cannot resolve view changes"))
        .when(viewType)
        .resolveViewChanges(view, viewChange);
    worker.start();
    worker.submit(view, viewType, viewChange);

    assertTimeoutPreemptively(TIMEOUT, () -> worker.awaitAppliedSubmissions(view));
  }

  @Test
  void awaitAppliedSubmissions_failsIfTheWorkerDoesNotRun() {
    worker.submit(view, viewType, viewChange);

    assertThrows(IllegalStateException.class, () -> worker.awaitAppliedSubmissions(view));
  }

  @Test
  void stop_endsTheWorkerThread() {
    worker.start();
    assertTrue(isWorkerThreadAlive());

    worker.stop();

    assertFalse(isWorkerThreadAlive());
  }

  @Test
  void stop_waitsForAStepThatOutlastsTheTimeoutAndIgnoresTheInterrupt() throws Exception {
    worker = new PropagationWorker(vsum, vsumLock, Duration.ofMillis(100));
    CountDownLatch stepRunning = new CountDownLatch(1);
    CountDownLatch finishStep = new CountDownLatch(1);
    AtomicBoolean stepFinished = new AtomicBoolean();
    step =
        first -> {
          stepRunning.countDown();
          // Like saving a model, this does not react to the interrupt.
          Uninterruptibles.awaitUninterruptibly(finishStep);
          stepFinished.set(true);
          return propagated(first);
        };
    worker.start();
    worker.submit(view, viewType, viewChange);
    assertTrue(stepRunning.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
    CountDownLatch stopped = new CountDownLatch(1);
    AtomicBoolean stepFinishedWhenStopped = new AtomicBoolean();
    Thread.ofPlatform()
        .start(
            () -> {
              worker.stop();
              stepFinishedWhenStopped.set(stepFinished.get());
              stopped.countDown();
            });

    assertFalse(stopped.await(500, TimeUnit.MILLISECONDS), "stop() returned during the step");
    finishStep.countDown();

    assertTrue(stopped.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
    assertTrue(stepFinishedWhenStopped.get());
    assertFalse(isWorkerThreadAlive());
  }

  /** Queues a change that this worker did not enqueue, as another user of the VSUM would. */
  private void enqueueForeignChange(int taskId) {
    synchronized (queue) {
      queue.add(new QueuedChange(taskId, null, taskId, null, null));
    }
  }

  /** Queues follow-up changes of the given change at the front, as a propagation step does. */
  private void addFollowUps(QueuedChange parent, int... taskIds) {
    synchronized (queue) {
      for (int i = taskIds.length - 1; i >= 0; i--) {
        queue.add(
            0, new QueuedChange(taskIds[i], parent.taskId(), parent.rootTaskId(), null, null));
      }
    }
  }

  private QueuedPropagationStep askUnlessAnswered(QueuedChange first) {
    synchronized (queue) {
      if (answersByRoot.containsKey(first.rootTaskId())) {
        return propagated(first);
      }
      queue.add(
          new QueuedChange(
              first.taskId(),
              first.parentTaskId(),
              first.rootTaskId(),
              PropagationBlockReason.USER_INTERACTION,
              QUESTION));
      return new QueuedPropagationStep(first.taskId(), Outcome.DEFERRED, List.of());
    }
  }

  private static QueuedPropagationStep propagated(QueuedChange first) {
    return new QueuedPropagationStep(first.taskId(), Outcome.PROPAGATED, List.of());
  }

  @SuppressWarnings("unchecked")
  private VitruviusChange<HierarchicalId> mockViewChange() {
    return mock(VitruviusChange.class);
  }

  /** Waits until the simulated VSUM has taken every queued change. */
  private void awaitNothingQueued() {
    long deadline = System.nanoTime() + TIMEOUT.toNanos();
    while (true) {
      synchronized (queue) {
        if (queue.isEmpty()) {
          return;
        }
        if (System.nanoTime() > deadline) {
          fail("Changes are still queued: " + queue);
        }
      }
      Thread.yield();
    }
  }

  private boolean isPending(int taskId) {
    synchronized (queue) {
      return queue.stream().anyMatch(it -> it.taskId() == taskId || it.rootTaskId() == taskId);
    }
  }

  private TaskStatus awaitState(int taskId, TaskState state) {
    return awaitStatus(taskId, status -> status.state() == state, state.toString());
  }

  private TaskStatus awaitStatus(int taskId, Predicate<TaskStatus> condition, String expected) {
    long deadline = System.nanoTime() + TIMEOUT.toNanos();
    TaskStatus status = worker.getStatus(taskId).orElseThrow();
    while (!condition.test(status)) {
      if (System.nanoTime() > deadline) {
        fail("Task " + taskId + " did not reach " + expected + ", last status: " + status);
      }
      Thread.yield();
      status = worker.getStatus(taskId).orElseThrow();
    }
    return status;
  }

  private static boolean isWorkerThreadAlive() {
    return Thread.getAllStackTraces().keySet().stream()
        .anyMatch(it -> it.getName().equals(PropagationWorker.THREAD_NAME) && it.isAlive());
  }
}
