package tools.vitruv.framework.remote.server.rest.endpoints;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.net.HttpURLConnection;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import tools.vitruv.change.atomic.hid.HierarchicalId;
import tools.vitruv.change.composite.description.VitruviusChangeFactory;
import tools.vitruv.change.interaction.PendingUserInteraction;
import tools.vitruv.change.propagation.PropagationBlockReason;
import tools.vitruv.change.propagation.QueuedChange;
import tools.vitruv.change.propagation.QueuedPropagationStep;
import tools.vitruv.change.propagation.QueuedPropagationStep.Outcome;
import tools.vitruv.change.utils.ProjectMarker;
import tools.vitruv.framework.remote.common.json.JsonMapper;
import tools.vitruv.framework.remote.common.rest.TaskState;
import tools.vitruv.framework.remote.common.rest.TaskStatus;
import tools.vitruv.framework.remote.common.rest.constants.ContentType;
import tools.vitruv.framework.remote.common.rest.constants.Header;
import tools.vitruv.framework.remote.server.exception.ServerHaltingException;
import tools.vitruv.framework.remote.server.propagation.PropagationWorker;
import tools.vitruv.framework.views.ViewSelector;
import tools.vitruv.framework.views.impl.ModifiableView;
import tools.vitruv.framework.views.impl.ViewCreatingViewType;
import tools.vitruv.framework.vsum.VirtualModel;

class ChangePropagationEndpointTest {
  private static final String VIEW_UUID = "change-propagation-endpoint-test-view";
  private static final String OTHER_VIEW_UUID = "change-propagation-endpoint-test-other-view";
  private static final Duration TIMEOUT = Duration.ofSeconds(10);
  private static final int VSUM_TASK_ID = 5;
  private static final PendingUserInteraction QUESTION =
      new PendingUserInteraction(
          PendingUserInteraction.Kind.CONFIRMATION,
          null,
          "Delete the class as well?",
          "Yes",
          "No",
          null,
          List.of());

  @TempDir Path vsumFolder;

  private final Object vsumLock = new Object();
  private final PropagationWorker worker = mock(PropagationWorker.class);
  private final ModifiableView view = mock(ModifiableView.class);
  private final ModifiableView otherView = mock(ModifiableView.class);

  @SuppressWarnings("unchecked")
  private final ViewCreatingViewType<ViewSelector, HierarchicalId> viewType =
      mock(ViewCreatingViewType.class);

  // The VSUM of a real worker, see startWorker.
  private final VirtualModel vsum = mock(VirtualModel.class);
  private final CountDownLatch propagateFollowUp = new CountDownLatch(1);
  private PropagationWorker realWorker;

  private JsonMapper mapper;
  private ChangePropagationEndpoint endpoint;
  private String changeJson;

  @BeforeEach
  void setUp() throws Exception {
    ProjectMarker.markAsProjectRootFolder(vsumFolder);
    mapper = new JsonMapper(vsumFolder);
    endpoint = new ChangePropagationEndpoint(mapper, vsumLock, worker);
    doReturn(viewType).when(view).getViewType();
    doReturn(viewType).when(otherView).getViewType();
    Cache.addView(VIEW_UUID, view);
    Cache.addView(OTHER_VIEW_UUID, otherView);
    changeJson =
        mapper.serialize(
            VitruviusChangeFactory.getInstance().<HierarchicalId>createCompositeChange(List.of()));
  }

  @AfterEach
  void removeViewsAndStopWorker() {
    Cache.removeView(VIEW_UUID);
    Cache.removeView(OTHER_VIEW_UUID);
    if (realWorker != null) {
      realWorker.stop();
    }
  }

  @Test
  void withoutPreference_propagatesBeforeAnsweringWhileHoldingTheVsumLock() throws Exception {
    doAnswer(
            invocation -> {
              assertTrue(Thread.holdsLock(vsumLock), "propagation must hold the VSUM lock");
              return null;
            })
        .when(viewType)
        .commitViewChanges(eq(view), any());
    var wrapper = new FakeHttpWrapper(changeJson).withHeader(Header.VIEW_UUID, VIEW_UUID);

    assertNull(endpoint.process(wrapper));

    verify(viewType).commitViewChanges(eq(view), any());
    verify(worker).awaitFinishedSubmissions(view);
    verifyNoMoreInteractions(worker);
    assertEquals(HttpURLConnection.HTTP_OK, wrapper.responseCode());
    assertNull(wrapper.responseHeader(Header.TASK_ID));
  }

  @Test
  void withoutPreference_firstWaitsForTheChangesTheViewSentAsynchronously() throws Exception {
    doAnswer(
            invocation -> {
              assertFalse(
                  Thread.holdsLock(vsumLock), "the worker needs the lock to propagate them");
              return null;
            })
        .when(worker)
        .awaitFinishedSubmissions(view);
    var wrapper = new FakeHttpWrapper(changeJson).withHeader(Header.VIEW_UUID, VIEW_UUID);

    endpoint.process(wrapper);

    InOrder inOrder = inOrder(worker, viewType);
    inOrder.verify(worker).awaitFinishedSubmissions(view);
    inOrder.verify(viewType).commitViewChanges(eq(view), any());
  }

  @Test
  void withoutPreference_isUnavailableWhileEarlierChangesCannotBePropagated() throws Exception {
    doThrow(new IllegalStateException("worker not running"))
        .when(worker)
        .awaitFinishedSubmissions(view);
    var wrapper = new FakeHttpWrapper(changeJson).withHeader(Header.VIEW_UUID, VIEW_UUID);

    var exception = assertThrows(ServerHaltingException.class, () -> endpoint.process(wrapper));

    assertEquals(HttpURLConnection.HTTP_UNAVAILABLE, exception.getStatusCode());
    verify(viewType, never()).commitViewChanges(any(), any());
  }

  @Test
  void withoutPreference_isAConflictWhileAChangeTheViewSentAsynchronouslyWaitsForInput()
      throws Exception {
    startWorker(true);
    int taskId = sendAsynchronously();
    awaitState(taskId, TaskState.WAITING_FOR_INPUT);

    var wrapper = new FakeHttpWrapper(changeJson).withHeader(Header.VIEW_UUID, VIEW_UUID);
    var exception =
        assertTimeoutPreemptively(
            TIMEOUT,
            () -> assertThrows(ServerHaltingException.class, () -> endpoint.process(wrapper)));

    assertEquals(HttpURLConnection.HTTP_CONFLICT, exception.getStatusCode());
    assertTrue(
        exception.getMessage().contains("task id " + taskId + " ")
            && exception.getMessage().contains("waits for user input"),
        exception.getMessage());
    verify(viewType, never()).commitViewChanges(any(), any());
    // Another view does not wait for the question.
    var otherViewRequest =
        new FakeHttpWrapper(changeJson).withHeader(Header.VIEW_UUID, OTHER_VIEW_UUID);
    assertTimeoutPreemptively(TIMEOUT, () -> endpoint.process(otherViewRequest));
    assertEquals(HttpURLConnection.HTTP_OK, otherViewRequest.responseCode());
    verify(viewType).commitViewChanges(eq(otherView), any());
  }

  @Test
  void withoutPreference_waitsForAChangeTheViewSentAsynchronouslyThatIsOnlyQueued()
      throws Exception {
    CountDownLatch resolve = new CountDownLatch(1);
    doAnswer(
            invocation -> {
              assertTrue(resolve.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
              return null;
            })
        .when(viewType)
        .resolveViewChanges(eq(view), any());
    startWorker(false);
    int taskId = sendAsynchronously();
    assertEquals(TaskState.QUEUED, realWorker.getStatus(taskId).orElseThrow().state());
    var wrapper = new FakeHttpWrapper(changeJson).withHeader(Header.VIEW_UUID, VIEW_UUID);
    FutureTask<String> request = new FutureTask<>(() -> endpoint.process(wrapper));
    Thread requestThread = Thread.ofPlatform().start(request);

    // The request waits without the VSUM lock, which the worker needs to propagate the change.
    awaitWaiting(requestThread);
    verify(viewType, never()).commitViewChanges(any(), any());
    resolve.countDown();

    // Applying the change and taking its first step is not enough: it has a follow-up change.
    verify(vsum, timeout(TIMEOUT.toMillis()).times(2)).propagateNextQueuedChange();
    assertThrows(TimeoutException.class, () -> request.get(500, TimeUnit.MILLISECONDS));
    verify(viewType, never()).commitViewChanges(any(), any());
    propagateFollowUp.countDown();

    assertNull(request.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
    assertEquals(HttpURLConnection.HTTP_OK, wrapper.responseCode());
    assertEquals(TaskState.DONE, realWorker.getStatus(taskId).orElseThrow().state());
    InOrder inOrder = inOrder(vsum, viewType);
    inOrder.verify(vsum, times(2)).propagateNextQueuedChange();
    inOrder.verify(viewType).commitViewChanges(eq(view), any());
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = Header.RESPOND_ASYNC)
  void withoutViewHeader_rejectsTheChangeLikeAnUnknownView(String preferHeader) {
    var wrapper = new FakeHttpWrapper(changeJson).withHeader(Header.PREFER, preferHeader);

    assertThrows(ServerHaltingException.class, () -> endpoint.process(wrapper));

    verifyNoInteractions(worker);
  }

  @Test
  void withRespondAsync_answersAcceptedWithTheTaskInsteadOfPropagating() throws Exception {
    var status = new TaskStatus(42, TaskState.QUEUED, null, null);
    when(worker.submit(eq(view), eq(viewType), any())).thenReturn(42);
    when(worker.getStatus(42)).thenReturn(Optional.of(status));
    var wrapper =
        new FakeHttpWrapper(changeJson)
            .withHeader(Header.VIEW_UUID, VIEW_UUID)
            .withHeader(Header.PREFER, "respond-async, wait=5");

    String body = endpoint.process(wrapper);

    assertEquals(status, mapper.deserialize(body, TaskStatus.class));
    assertEquals(HttpURLConnection.HTTP_ACCEPTED, wrapper.responseCode());
    assertEquals(Header.RESPOND_ASYNC, wrapper.responseHeader(Header.PREFERENCE_APPLIED));
    assertEquals("42", wrapper.responseHeader(Header.TASK_ID));
    assertEquals(ContentType.APPLICATION_JSON, wrapper.contentType());
    verify(viewType, never()).commitViewChanges(any(), any());
  }

  @Test
  void withRespondAsync_doesNotWaitForTheVsumLock() throws InterruptedException {
    when(worker.submit(eq(view), eq(viewType), any())).thenReturn(42);
    when(worker.getStatus(42))
        .thenReturn(Optional.of(new TaskStatus(42, TaskState.QUEUED, null, null)));
    var wrapper =
        new FakeHttpWrapper(changeJson)
            .withHeader(Header.VIEW_UUID, VIEW_UUID)
            .withHeader(Header.PREFER, Header.RESPOND_ASYNC);
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

    try {
      assertTimeoutPreemptively(TIMEOUT, () -> endpoint.process(wrapper));
    } finally {
      releaseLock.countDown();
      lockHolder.join();
    }
    assertEquals(HttpURLConnection.HTTP_ACCEPTED, wrapper.responseCode());
  }

  @Test
  void withRespondAsync_rejectsAnInvalidChangeLikeBefore() {
    var wrapper =
        new FakeHttpWrapper("{ not a change")
            .withHeader(Header.VIEW_UUID, VIEW_UUID)
            .withHeader(Header.PREFER, Header.RESPOND_ASYNC);

    var exception = assertThrows(ServerHaltingException.class, () -> endpoint.process(wrapper));

    assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, exception.getStatusCode());
    verifyNoInteractions(worker);
  }

  @Test
  void withRespondAsync_rejectsAnUnknownViewLikeBefore() {
    var wrapper =
        new FakeHttpWrapper(changeJson)
            .withHeader(Header.VIEW_UUID, "unknown")
            .withHeader(Header.PREFER, Header.RESPOND_ASYNC);

    assertThrows(ServerHaltingException.class, () -> endpoint.process(wrapper));

    verifyNoInteractions(worker);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "respond-async",
        "Respond-Async",
        " respond-async ",
        "wait=10, respond-async",
        "respond-async; foo=bar",
        "return=minimal,respond-async"
      })
  void prefersAsync_findsTheRespondAsyncPreference(String preferHeader) {
    assertTrue(ChangePropagationEndpoint.prefersAsync(preferHeader));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"wait=10", "return=minimal", "respond-asynchronously", "handling=strict"})
  void prefersAsync_ignoresOtherPreferences(String preferHeader) {
    assertFalse(ChangePropagationEndpoint.prefersAsync(preferHeader));
  }

  /**
   * Lets the endpoint work with a real worker and its thread. The worker's VSUM queues one change
   * at a time. If {@code askForInput} is set, a step lets the change wait for an answer to {@link
   * #QUESTION}. Otherwise, the first step propagates the change and queues a follow-up change, and
   * the second step propagates that once {@link #propagateFollowUp} is counted down.
   */
  private void startWorker(boolean askForInput) {
    AtomicReference<List<QueuedChange>> queue = new AtomicReference<>(List.of());
    when(vsum.enqueueChange(any()))
        .thenAnswer(
            invocation -> {
              queue.set(List.of(new QueuedChange(VSUM_TASK_ID, null, VSUM_TASK_ID, null, null)));
              return VSUM_TASK_ID;
            });
    when(vsum.getQueuedChanges()).thenAnswer(invocation -> queue.get());
    when(vsum.propagateNextQueuedChange())
        .thenAnswer(
            invocation -> {
              if (askForInput) {
                queue.set(
                    List.of(
                        new QueuedChange(
                            VSUM_TASK_ID,
                            null,
                            VSUM_TASK_ID,
                            PropagationBlockReason.USER_INTERACTION,
                            QUESTION)));
                return Optional.of(
                    new QueuedPropagationStep(VSUM_TASK_ID, Outcome.DEFERRED, List.of()));
              }
              int taskId = queue.get().get(0).taskId();
              if (taskId == VSUM_TASK_ID) {
                int followUp = VSUM_TASK_ID + 1;
                queue.set(
                    List.of(new QueuedChange(followUp, VSUM_TASK_ID, VSUM_TASK_ID, null, null)));
              } else {
                assertTrue(propagateFollowUp.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
                queue.set(List.of());
              }
              return Optional.of(new QueuedPropagationStep(taskId, Outcome.PROPAGATED, List.of()));
            });
    realWorker = new PropagationWorker(vsum, vsumLock);
    realWorker.start();
    endpoint = new ChangePropagationEndpoint(mapper, vsumLock, realWorker);
  }

  /** Sends a change of the view for asynchronous propagation and returns its task id. */
  private int sendAsynchronously() {
    var wrapper =
        new FakeHttpWrapper(changeJson)
            .withHeader(Header.VIEW_UUID, VIEW_UUID)
            .withHeader(Header.PREFER, Header.RESPOND_ASYNC);
    endpoint.process(wrapper);
    assertEquals(HttpURLConnection.HTTP_ACCEPTED, wrapper.responseCode());
    return Integer.parseInt(wrapper.responseHeader(Header.TASK_ID));
  }

  private void awaitState(int taskId, TaskState state) {
    long deadline = System.nanoTime() + TIMEOUT.toNanos();
    TaskStatus status = realWorker.getStatus(taskId).orElseThrow();
    while (status.state() != state) {
      if (System.nanoTime() > deadline) {
        fail("Task " + taskId + " did not reach " + state + ", last status: " + status);
      }
      Thread.yield();
      status = realWorker.getStatus(taskId).orElseThrow();
    }
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
}
