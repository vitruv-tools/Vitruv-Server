package tools.vitruv.framework.remote.server.propagation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
  private final ModifiableView otherView = mock(ModifiableView.class);

  @SuppressWarnings("unchecked")
  private final ViewCreatingViewType<?, HierarchicalId> viewType = mock(ViewCreatingViewType.class);

  @SuppressWarnings("unchecked")
  private final VitruviusChange<HierarchicalId> viewChange = mock(VitruviusChange.class);

  @SuppressWarnings("unchecked")
  private final VitruviusChange<Uuid> resolvedChange = mock(VitruviusChange.class);

  private final UserInteractionBase answer = mock(UserInteractionBase.class);

  // The queue of the simulated VSUM, each part of a change with its own block; all access is
  // synchronized on it. Like the VSUM, it propagates the parts of a change in order, so while one
  // of them is blocked, the others wait with it.
  private final List<QueuedChange> queue = new ArrayList<>();

  // The answers that no step has used yet, by enqueued change.
  private final Map<Integer, List<UserInteractionBase>> answersByRoot = new ConcurrentHashMap<>();
  private final List<Boolean> lockHeldInVsum = new CopyOnWriteArrayList<>();

  // The simulated VSUM picks its own task ids, far away from anything a test uses otherwise.
  private final AtomicInteger nextVsumTaskId = new AtomicInteger(1_000_000);
  private final List<Integer> vsumTaskIds = new CopyOnWriteArrayList<>();

  // What the simulated VSUM was asked to do, in order.
  private final List<String> events = new CopyOnWriteArrayList<>();

  // What a propagation step of the simulated VSUM does with the first queued change, which has
  // already been taken off the queue. By default, it is propagated completely. If it throws, the
  // whole change is dropped, as the VSUM does, unless it throws a FailureAfterStep.
  private volatile FakeStep step = PropagationWorkerTest::propagated;

  private PropagationWorker worker;

  @FunctionalInterface
  private interface FakeStep {
    QueuedPropagationStep take(QueuedChange first);
  }

  /**
   * Thrown by a fake step that has gone through, as when the VSUM cannot save the models after the
   * step. The VSUM leaves its queue as the step left it then.
   */
  private static final class FailureAfterStep extends RuntimeException {
    private FailureAfterStep(String message) {
      super(message);
    }
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
    when(vsum.getQueuedChanges()).thenAnswer(invocation -> snapshotOfQueue());
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
                first = queue.get(0);
                if (blockOfChange(first.rootTaskId()) != null) {
                  moveChangeToEnd(first.rootTaskId());
                  return Optional.of(
                      new QueuedPropagationStep(
                          first.taskId(), Outcome.SKIPPED_BLOCKED, List.of()));
                }
                queue.remove(0);
              }
              events.add("step " + first.taskId());
              try {
                return Optional.of(step.take(first));
              } catch (FailureAfterStep e) {
                throw e;
              } catch (RuntimeException e) {
                synchronized (queue) {
                  queue.removeIf(it -> it.rootTaskId() == first.rootTaskId());
                }
                throw e;
              }
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
  void submissionsOfOneView_areResolvedOnceTheChangeBeforeHasBeenPropagatedCompletely() {
    // Queued by someone else, so the first submitted change is not the next one to propagate.
    enqueueForeignChange(42);
    step =
        first -> {
          if (first.taskId() == vsumTaskIds.get(0)) {
            // The first change produces a follow-up change, which is propagated next.
            addFollowUps(first, first.taskId() + 1);
          }
          return propagated(first);
        };
    VitruviusChange<HierarchicalId> laterViewChange = mockViewChange();
    // Both submissions are there when the worker starts, as when it is busy while they come in.
    int firstTask = worker.submit(view, viewType, viewChange);
    int secondTask = worker.submit(view, viewType, laterViewChange);

    worker.start();
    awaitState(firstTask, TaskState.DONE);
    awaitState(secondTask, TaskState.DONE);

    // The second change is only resolved once the first one and its follow-up change have been
    // propagated, so it is resolved against models that contain the first one, and the reactions
    // to it find what the reactions to the first one created.
    assertEquals(
        List.of(
            "resolve " + viewChange,
            "enqueue " + vsumTaskIds.get(0),
            "step 42",
            "step " + vsumTaskIds.get(0),
            "step " + (vsumTaskIds.get(0) + 1),
            "resolve " + laterViewChange,
            "enqueue " + vsumTaskIds.get(1),
            "step " + vsumTaskIds.get(1)),
        events);
  }

  @Test
  void laterChangeOfAView_waitsWhileTheChangeBeforeWaitsForInput_butChangesOfOtherViewsGoOn() {
    // Only the change submitted first asks for input.
    step =
        first ->
            first.rootTaskId() == vsumTaskIds.get(0) ? askUnlessAnswered(first) : propagated(first);
    VitruviusChange<HierarchicalId> secondViewChange = mockViewChange();
    VitruviusChange<HierarchicalId> otherViewChange = mockViewChange();
    worker.start();

    int firstTask = worker.submit(view, viewType, viewChange);
    int secondTask = worker.submit(view, viewType, secondViewChange);
    int otherViewTask = worker.submit(otherView, viewType, otherViewChange);
    awaitState(firstTask, TaskState.WAITING_FOR_INPUT);
    awaitState(otherViewTask, TaskState.DONE);

    // The change of the other view does not wait for the question, although it was submitted
    // later, but the second change of the view is not even resolved while the first one waits.
    assertEquals(TaskState.QUEUED, worker.getStatus(secondTask).orElseThrow().state());
    assertEquals(
        List.of("resolve " + viewChange, "resolve " + otherViewChange),
        events.stream().filter(it -> it.startsWith("resolve ")).toList());

    worker.registerUserInteractions(firstTask, List.of(answer));

    awaitState(secondTask, TaskState.DONE);
    assertEquals(TaskState.DONE, worker.getStatus(firstTask).orElseThrow().state());
    assertTrue(
        events.lastIndexOf("step " + vsumTaskIds.get(0))
            < events.indexOf("resolve " + secondViewChange),
        "unexpected order: " + events);
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
  void status_hasOneQuestionAtATime() {
    PendingUserInteraction secondQuestion =
        new PendingUserInteraction(
            PendingUserInteraction.Kind.CONFIRMATION,
            null,
            "Delete the class as well?",
            "Yes",
            "No",
            null,
            List.of());
    step =
        first -> {
          int root = first.rootTaskId();
          if (first.taskId() == root) {
            // Two specifications react to the change, and each of them asks something.
            addFollowUps(first, root + 1, root + 2);
            return propagated(first);
          }
          return askUnlessAnswered(first, first.taskId() == root + 1 ? QUESTION : secondQuestion);
        };
    worker.start();
    int taskId = worker.submit(view, viewType, viewChange);

    // The second follow-up waits for the answer to the first one before it can ask.
    assertEquals(
        new TaskStatus(taskId, TaskState.WAITING_FOR_INPUT, QUESTION, null),
        awaitState(taskId, TaskState.WAITING_FOR_INPUT));

    worker.registerUserInteractions(taskId, List.of(answer));

    // The first follow-up has used up the answer, so the second one asks next.
    assertEquals(
        new TaskStatus(taskId, TaskState.WAITING_FOR_INPUT, secondQuestion, null),
        awaitStatus(
            taskId, status -> secondQuestion.equals(status.question()), "the second question"));
    worker.registerUserInteractions(taskId, List.of(answer));
    awaitState(taskId, TaskState.DONE);
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
  void failedTask_hasNoQuestionAndTakesNoAnswers() {
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

    // Like a synchronous propagation, the change ends at the failure, so the second follow-up is
    // dropped before it can ask anything.
    TaskStatus failed = awaitState(taskId, TaskState.FAILED);
    assertEquals(new TaskStatus(taskId, TaskState.FAILED, null, "reaction failed"), failed);

    assertThrows(
        IllegalStateException.class,
        () -> worker.registerUserInteractions(taskId, List.of(answer)));
    // A change made to the view afterwards does not wait for the failed one.
    assertTimeoutPreemptively(TIMEOUT, () -> worker.awaitFinishedSubmissions(view));
    assertEquals(failed, worker.getStatus(taskId).orElseThrow());
  }

  @Test
  void failureAfterAStepThatWentThrough_doesNotFailTheChange_whichGoesOnInOrder() {
    VitruviusChange<HierarchicalId> laterViewChange = mockViewChange();
    step =
        first -> {
          int firstChange = vsumTaskIds.get(0);
          if (first.taskId() == firstChange) {
            // The change produces a follow-up change, and then saving the models fails.
            addFollowUps(first, firstChange + 1);
            throw new FailureAfterStep("Could not save URI x");
          }
          return first.taskId() == firstChange + 1 ? askUnlessAnswered(first) : propagated(first);
        };
    worker.start();
    int taskId = worker.submit(view, viewType, viewChange);
    int laterTaskId = worker.submit(view, viewType, laterViewChange);

    // The follow-up change is still queued, so its question reaches the client ...
    assertEquals(
        new TaskStatus(taskId, TaskState.WAITING_FOR_INPUT, QUESTION, null),
        awaitState(taskId, TaskState.WAITING_FOR_INPUT));
    // ... and the later change of the view waits for it.
    assertEquals(TaskState.QUEUED, worker.getStatus(laterTaskId).orElseThrow().state());
    assertTimeoutPreemptively(
        TIMEOUT,
        () ->
            assertThrows(
                WaitingForInputException.class, () -> worker.awaitFinishedSubmissions(view)));

    worker.registerUserInteractions(taskId, List.of(answer));

    awaitState(laterTaskId, TaskState.DONE);
    assertEquals(TaskState.DONE, worker.getStatus(taskId).orElseThrow().state());
    assertTrue(
        events.lastIndexOf("step " + (vsumTaskIds.get(0) + 1))
            < events.indexOf("resolve " + laterViewChange),
        "unexpected order: " + events);
  }

  @Test
  void failureAfterTheLastStepOfAChange_failsIt() {
    step =
        first -> {
          throw new FailureAfterStep("Could not save URI x");
        };
    worker.start();

    int taskId = worker.submit(view, viewType, viewChange);

    // Nothing of the change is left to propagate, so the models may never be saved.
    assertEquals(
        new TaskStatus(taskId, TaskState.FAILED, null, "Could not save URI x"),
        awaitState(taskId, TaskState.FAILED));
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
  void awaitFinishedSubmissions_waitsUntilTheChangesOfTheViewHaveBeenPropagated() throws Exception {
    CountDownLatch resolve = new CountDownLatch(1);
    doAnswer(
            invocation -> {
              assertTrue(resolve.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
              return resolvedChange;
            })
        .when(viewType)
        .resolveViewChanges(view, viewChange);
    CountDownLatch followUpRunning = new CountDownLatch(1);
    CountDownLatch finishFollowUp = new CountDownLatch(1);
    step =
        first -> {
          if (first.taskId() == first.rootTaskId()) {
            // The change produces a follow-up change, which has to be propagated as well.
            addFollowUps(first, first.taskId() + 1);
          } else {
            followUpRunning.countDown();
            Uninterruptibles.awaitUninterruptibly(finishFollowUp, TIMEOUT);
          }
          return propagated(first);
        };
    worker.start();
    int taskId = worker.submit(view, viewType, viewChange);
    FutureTask<Void> request =
        new FutureTask<>(
            () -> {
              worker.awaitFinishedSubmissions(view);
              return null;
            });
    Thread requestThread = Thread.ofPlatform().start(request);

    // Requests of other views have nothing to wait for.
    assertTimeoutPreemptively(TIMEOUT, () -> worker.awaitFinishedSubmissions(otherView));
    // The change is not even resolved yet, but it does not wait for input either.
    awaitWaiting(requestThread);
    assertEquals(TaskState.QUEUED, worker.getStatus(taskId).orElseThrow().state());
    resolve.countDown();

    // The change has been applied and its first step taken, but its follow-up change is still
    // being propagated, so the request goes on waiting.
    assertTrue(followUpRunning.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
    assertThrows(TimeoutException.class, () -> request.get(500, TimeUnit.MILLISECONDS));
    finishFollowUp.countDown();

    request.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
    assertEquals(TaskState.DONE, worker.getStatus(taskId).orElseThrow().state());
  }

  @Test
  void awaitFinishedSubmissions_givesUpWhileAChangeOfTheViewWaitsForInput() {
    step = this::askUnlessAnswered;
    worker.start();
    int waiting = worker.submit(view, viewType, viewChange);
    awaitState(waiting, TaskState.WAITING_FOR_INPUT);

    WaitingForInputException exception =
        assertTimeoutPreemptively(
            TIMEOUT,
            () ->
                assertThrows(
                    WaitingForInputException.class, () -> worker.awaitFinishedSubmissions(view)));

    assertEquals(waiting, exception.getTaskId());
    // Requests of other views are not affected.
    assertTimeoutPreemptively(TIMEOUT, () -> worker.awaitFinishedSubmissions(otherView));
  }

  @Test
  void awaitFinishedSubmissions_givesUpOnceAChangeOfTheViewStartsToWaitForInput() throws Exception {
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
    FutureTask<Void> request =
        new FutureTask<>(
            () -> {
              worker.awaitFinishedSubmissions(view);
              return null;
            });
    Thread requestThread = Thread.ofPlatform().start(request);
    awaitWaiting(requestThread);

    resolve.countDown();

    ExecutionException exception =
        assertThrows(
            ExecutionException.class, () -> request.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
    assertEquals(
        taskId, assertInstanceOf(WaitingForInputException.class, exception.getCause()).getTaskId());
    assertEquals(TaskState.WAITING_FOR_INPUT, worker.getStatus(taskId).orElseThrow().state());
  }

  @Test
  void awaitFinishedSubmissions_doesNotWaitForAChangeThatCouldNotBeResolved() {
    doThrow(new UnsupportedOperationException("cannot resolve view changes"))
        .when(viewType)
        .resolveViewChanges(view, viewChange);
    worker.start();
    worker.submit(view, viewType, viewChange);

    assertTimeoutPreemptively(TIMEOUT, () -> worker.awaitFinishedSubmissions(view));
  }

  @Test
  void awaitFinishedSubmissions_failsIfTheWorkerDoesNotRun() {
    worker.submit(view, viewType, viewChange);

    assertThrows(IllegalStateException.class, () -> worker.awaitFinishedSubmissions(view));
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
    return askUnlessAnswered(first, QUESTION);
  }

  /**
   * Propagates the change if an answer for its enqueued change is left, which uses the answer up.
   * Otherwise defers it like the VSUM: the change waits with the rest of its enqueued change at the
   * end of the queue.
   */
  private QueuedPropagationStep askUnlessAnswered(
      QueuedChange first, PendingUserInteraction question) {
    synchronized (queue) {
      List<UserInteractionBase> answers = answersByRoot.get(first.rootTaskId());
      if (answers != null && !answers.isEmpty()) {
        answers.remove(0);
        return propagated(first);
      }
      queue.add(
          0,
          new QueuedChange(
              first.taskId(),
              first.parentTaskId(),
              first.rootTaskId(),
              PropagationBlockReason.USER_INTERACTION,
              question));
      moveChangeToEnd(first.rootTaskId());
      return new QueuedPropagationStep(first.taskId(), Outcome.DEFERRED, List.of());
    }
  }

  private static QueuedPropagationStep propagated(QueuedChange first) {
    return new QueuedPropagationStep(first.taskId(), Outcome.PROPAGATED, List.of());
  }

  /**
   * Returns the queued changes as the VSUM reports them: a change that waits for a blocked change
   * of the same enqueued change has the block reason of that change, but not its question.
   */
  private List<QueuedChange> snapshotOfQueue() {
    synchronized (queue) {
      return queue.stream()
          .map(
              it ->
                  it.blocked()
                      ? it
                      : new QueuedChange(
                          it.taskId(),
                          it.parentTaskId(),
                          it.rootTaskId(),
                          blockOfChange(it.rootTaskId()),
                          null))
          .toList();
    }
  }

  /** Returns the block of a queued change of the given enqueued change, {@code null} if none. */
  private PropagationBlockReason blockOfChange(int rootTaskId) {
    synchronized (queue) {
      return queue.stream()
          .filter(it -> it.rootTaskId() == rootTaskId && it.blocked())
          .map(QueuedChange::blockReason)
          .findFirst()
          .orElse(null);
    }
  }

  /**
   * Moves the queued changes of an enqueued change to the end of the queue, keeping their order.
   */
  private void moveChangeToEnd(int rootTaskId) {
    synchronized (queue) {
      List<QueuedChange> parts =
          queue.stream().filter(it -> it.rootTaskId() == rootTaskId).toList();
      queue.removeIf(it -> it.rootTaskId() == rootTaskId);
      queue.addAll(parts);
    }
  }

  @SuppressWarnings("unchecked")
  private VitruviusChange<HierarchicalId> mockViewChange() {
    return mock(VitruviusChange.class);
  }

  /** Waits until the thread waits, e.g., for the changes of a view to finish. */
  private static void awaitWaiting(Thread thread) {
    long deadline = System.nanoTime() + TIMEOUT.toNanos();
    while (thread.getState() != Thread.State.WAITING) {
      if (System.nanoTime() > deadline) {
        fail("Thread does not wait, state: " + thread.getState());
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
