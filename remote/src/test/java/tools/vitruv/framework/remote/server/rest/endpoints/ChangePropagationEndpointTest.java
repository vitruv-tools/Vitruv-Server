package tools.vitruv.framework.remote.server.rest.endpoints;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import java.util.concurrent.TimeUnit;
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

class ChangePropagationEndpointTest {
  private static final String VIEW_UUID = "change-propagation-endpoint-test-view";
  private static final Duration TIMEOUT = Duration.ofSeconds(10);

  @TempDir Path vsumFolder;

  private final Object vsumLock = new Object();
  private final PropagationWorker worker = mock(PropagationWorker.class);
  private final ModifiableView view = mock(ModifiableView.class);

  @SuppressWarnings("unchecked")
  private final ViewCreatingViewType<ViewSelector, HierarchicalId> viewType =
      mock(ViewCreatingViewType.class);

  private JsonMapper mapper;
  private ChangePropagationEndpoint endpoint;
  private String changeJson;

  @BeforeEach
  void setUp() throws Exception {
    ProjectMarker.markAsProjectRootFolder(vsumFolder);
    mapper = new JsonMapper(vsumFolder);
    endpoint = new ChangePropagationEndpoint(mapper, vsumLock, worker);
    doReturn(viewType).when(view).getViewType();
    Cache.addView(VIEW_UUID, view);
    changeJson =
        mapper.serialize(
            VitruviusChangeFactory.getInstance().<HierarchicalId>createCompositeChange(List.of()));
  }

  @AfterEach
  void removeView() {
    Cache.removeView(VIEW_UUID);
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
    verify(worker).awaitAppliedSubmissions(view);
    verifyNoMoreInteractions(worker);
    assertEquals(HttpURLConnection.HTTP_OK, wrapper.responseCode());
    assertNull(wrapper.responseHeader(Header.TASK_ID));
  }

  @Test
  void withoutPreference_firstWaitsForTheChangesTheViewSentAsynchronously() throws Exception {
    doAnswer(
            invocation -> {
              assertFalse(Thread.holdsLock(vsumLock), "the worker needs the lock to apply them");
              return null;
            })
        .when(worker)
        .awaitAppliedSubmissions(view);
    var wrapper = new FakeHttpWrapper(changeJson).withHeader(Header.VIEW_UUID, VIEW_UUID);

    endpoint.process(wrapper);

    InOrder inOrder = inOrder(worker, viewType);
    inOrder.verify(worker).awaitAppliedSubmissions(view);
    inOrder.verify(viewType).commitViewChanges(eq(view), any());
  }

  @Test
  void withoutPreference_isUnavailableWhileEarlierChangesCannotBeApplied() throws Exception {
    doThrow(new IllegalStateException("worker not running"))
        .when(worker)
        .awaitAppliedSubmissions(view);
    var wrapper = new FakeHttpWrapper(changeJson).withHeader(Header.VIEW_UUID, VIEW_UUID);

    var exception = assertThrows(ServerHaltingException.class, () -> endpoint.process(wrapper));

    assertEquals(HttpURLConnection.HTTP_UNAVAILABLE, exception.getStatusCode());
    verify(viewType, never()).commitViewChanges(any(), any());
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
}
