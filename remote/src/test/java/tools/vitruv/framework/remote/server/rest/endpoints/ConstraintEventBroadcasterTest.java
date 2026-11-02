package tools.vitruv.framework.remote.server.rest.endpoints;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.vitruv.framework.remote.common.json.JsonMapper;
import tools.vitruv.framework.remote.server.http.EventStream;
import tools.vitruv.framework.vsum.VirtualModel;
import tools.vitruv.framework.vsum.constraints.ConstraintDescriptor;
import tools.vitruv.framework.vsum.constraints.ConstraintSeverity;
import tools.vitruv.framework.vsum.constraints.ConstraintStatus;
import tools.vitruv.framework.vsum.constraints.ConstraintValidationListener.Trigger;
import tools.vitruv.framework.vsum.constraints.ConstraintValidationReport;
import tools.vitruv.framework.vsum.constraints.ConstraintValidationResult;

/** The events, keep-alive comments and subscriber handling of the {@link ConstraintEventBroadcaster}. */
class ConstraintEventBroadcasterTest {
  private static final long TIMEOUT_MILLIS = 5000;
  private static final long NO_HEARTBEAT = TimeUnit.HOURS.toMillis(1);
  private static final ConstraintDescriptor CONSTRAINT =
      new ConstraintDescriptor(
          "ecore::EPackage::lowercase", null, "ecore::EPackage", null,
          ConstraintSeverity.CRITICAL, "test", null);
  private static final ConstraintValidationReport VIOLATED =
      new ConstraintValidationReport(
          List.of(
              new ConstraintValidationResult(
                  CONSTRAINT, ConstraintStatus.VIOLATED, List.of(), List.of())));

  @TempDir Path folder;

  private VirtualModel model;
  private JsonMapper mapper;
  private ConstraintEventBroadcaster broadcaster;

  @BeforeEach
  void createModel() {
    model = mock(VirtualModel.class);
    when(model.getConstraints()).thenReturn(List.of(CONSTRAINT));
    mapper = new JsonMapper(folder);
  }

  @AfterEach
  void closeBroadcaster() {
    if (broadcaster != null) {
      broadcaster.close();
    }
  }

  @Test
  void announcesChangedConstraintsBeforeTheirValidation() throws Exception {
    broadcaster = new ConstraintEventBroadcaster(model, mapper, NO_HEARTBEAT);
    RecordingStream stream = subscribe();

    broadcaster.constraintsValidated(VIOLATED, Trigger.CONSTRAINTS_CHANGED);

    String revision = ConstraintRequests.revisionOf(model);
    Event constraints = stream.next();
    assertEquals(ConstraintEventBroadcaster.CONSTRAINTS_EVENT, constraints.name());
    assertTrue(constraints.data().contains(revision.replace("\"", "\\\"")), constraints.data());
    Event validation = stream.next();
    assertEquals(ConstraintEventBroadcaster.VALIDATION_EVENT, validation.name());
    assertTrue(validation.data().contains("VIOLATED"), validation.data());
    assertTrue(Long.parseLong(validation.id()) > Long.parseLong(constraints.id()));
  }

  @Test
  void sendsOnlyTheValidationAfterPropagationAndOnRequest() throws Exception {
    broadcaster = new ConstraintEventBroadcaster(model, mapper, NO_HEARTBEAT);
    RecordingStream stream = subscribe();

    broadcaster.constraintsValidated(VIOLATED, Trigger.PROPAGATION);
    broadcaster.constraintsValidated(VIOLATED, Trigger.REQUEST);

    assertEquals(ConstraintEventBroadcaster.VALIDATION_EVENT, stream.next().name());
    assertEquals(ConstraintEventBroadcaster.VALIDATION_EVENT, stream.next().name());
    assertTrue(stream.events.isEmpty());
  }

  @Test
  void sendsNothingWithoutSubscribers() throws Exception {
    broadcaster = new ConstraintEventBroadcaster(model, mapper, NO_HEARTBEAT);

    broadcaster.constraintsValidated(VIOLATED, Trigger.CONSTRAINTS_CHANGED);

    assertEquals(0, broadcaster.getSubscriberCount());
    verify(model, never()).getConstraints();
  }

  @Test
  void keepsIdleConnectionsOpenWithComments() throws Exception {
    broadcaster = new ConstraintEventBroadcaster(model, mapper, 20);
    EventStream stream = mock(EventStream.class);

    broadcaster.subscribe(stream);

    verify(stream, timeout(TIMEOUT_MILLIS).atLeast(2)).comment("heartbeat");
  }

  @Test
  void keepsSendingHeartbeatsWhenASubscriberFailsUnexpectedly() throws Exception {
    broadcaster = new ConstraintEventBroadcaster(model, mapper, 20);
    EventStream failing = mock(EventStream.class);
    doThrow(new IllegalStateException("broken")).when(failing).comment(anyString());
    EventStream healthy = mock(EventStream.class);

    broadcaster.subscribe(failing);
    broadcaster.subscribe(healthy);

    verify(failing, timeout(TIMEOUT_MILLIS)).close();
    verify(healthy, timeout(TIMEOUT_MILLIS).atLeast(3)).comment("heartbeat");
    assertEquals(1, broadcaster.getSubscriberCount());
  }

  @Test
  void dropsUnreachableSubscribersAndRestoresTheSetting() throws Exception {
    when(model.isConstraintValidationAfterPropagation()).thenReturn(false);
    broadcaster = new ConstraintEventBroadcaster(model, mapper, NO_HEARTBEAT);
    EventStream unreachable = mock(EventStream.class);
    doThrow(new IOException("gone")).when(unreachable).send(
        anyString(),
        any(),
        anyString());
    broadcaster.subscribe(unreachable);
    verify(model).setConstraintValidationAfterPropagation(true);

    broadcaster.constraintsValidated(VIOLATED, Trigger.PROPAGATION);

    verify(unreachable, timeout(TIMEOUT_MILLIS)).close();
    assertEquals(0, broadcaster.getSubscriberCount());
    verify(model, timeout(TIMEOUT_MILLIS)).setConstraintValidationAfterPropagation(false);
  }

  @Test
  void worksWithVirtualModelsWithoutValidationListeners() throws Exception {
    doThrow(new UnsupportedOperationException("no listeners"))
        .when(model)
        .addConstraintValidationListener(any());
    broadcaster = new ConstraintEventBroadcaster(model, mapper, NO_HEARTBEAT);

    broadcaster.subscribe(mock(EventStream.class));

    assertEquals(1, broadcaster.getSubscriberCount());
    verify(model, never()).setConstraintValidationAfterPropagation(true);
    broadcaster.close();
    verify(model, never()).removeConstraintValidationListener(broadcaster);
    broadcaster = null;
  }

  @Test
  void closingEndsAllSubscriptions() throws Exception {
    broadcaster = new ConstraintEventBroadcaster(model, mapper, NO_HEARTBEAT);
    EventStream stream = mock(EventStream.class);
    broadcaster.subscribe(stream);

    broadcaster.close();

    verify(stream).close();
    assertEquals(0, broadcaster.getSubscriberCount());
    verify(model).removeConstraintValidationListener(broadcaster);
    broadcaster = null;
  }

  /** Subscribes a recording stream; the validation for the new subscriber is not sent by the mock. */
  private RecordingStream subscribe() {
    RecordingStream stream = new RecordingStream();
    broadcaster.subscribe(stream);
    verify(model, timeout(TIMEOUT_MILLIS)).validateConstraints();
    return stream;
  }

  private record Event(String name, String id, String data) {}

  private static final class RecordingStream implements EventStream {
    private final BlockingQueue<Event> events = new LinkedBlockingQueue<>();
    private boolean closed;

    @Override
    public void send(String event, String id, String data) {
      events.add(new Event(event, id, data));
    }

    @Override
    public void comment(String comment) {
      // keep-alive comments are not recorded
    }

    @Override
    public void close() {
      closed = true;
    }

    Event next() throws InterruptedException {
      Event event = events.poll(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
      assertNotNull(event, "no event received");
      assertFalse(closed, "the stream was closed");
      return event;
    }
  }
}
