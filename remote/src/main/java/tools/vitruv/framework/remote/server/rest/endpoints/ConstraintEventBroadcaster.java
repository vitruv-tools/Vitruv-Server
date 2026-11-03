package tools.vitruv.framework.remote.server.rest.endpoints;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tools.vitruv.framework.remote.common.json.JsonMapper;
import tools.vitruv.framework.remote.common.rest.constraints.ConstraintRevisionDto;
import tools.vitruv.framework.remote.common.rest.constraints.ConstraintStatusDto;
import tools.vitruv.framework.remote.server.http.EventStream;
import tools.vitruv.framework.vsum.VirtualModel;
import tools.vitruv.framework.vsum.constraints.ConstraintValidationListener;
import tools.vitruv.framework.vsum.constraints.ConstraintValidationReport;
import tools.vitruv.framework.vsum.constraints.ConstraintValidationResult;

/**
 * Sends the results of constraint validations of the virtual model as Server-Sent Events to all
 * subscribed clients:
 *
 * <ul>
 *   <li>{@code validation} with a {@link ConstraintStatusDto} after all constraints were validated
 *   <li>{@code constraints} with a {@link ConstraintRevisionDto} after a constraint was created,
 *       updated or deleted
 * </ul>
 *
 * <p>While clients are subscribed, the virtual model validates its constraints after every
 * propagated change. Events are sent on an own thread, so slow clients do not delay the virtual
 * model; clients that cannot be reached are dropped. A comment is sent regularly to keep idle
 * connections open.
 */
public final class ConstraintEventBroadcaster
    implements ConstraintValidationListener, AutoCloseable {
  static final String VALIDATION_EVENT = "validation";
  static final String CONSTRAINTS_EVENT = "constraints";
  private static final Logger LOGGER = LogManager.getLogger(ConstraintEventBroadcaster.class);
  private static final long HEARTBEAT_MILLIS = TimeUnit.SECONDS.toMillis(15);

  private final VirtualModel model;
  private final JsonMapper mapper;
  private final List<EventStream> subscribers = new CopyOnWriteArrayList<>();
  private final ScheduledExecutorService sender;
  private final AtomicLong eventIds = new AtomicLong();
  private final boolean supported;

  /** The setting of the virtual model before the first client subscribed, null without clients. */
  private Boolean settingWithoutSubscribers;

  /**
   * Creates a broadcaster for the given virtual model and registers it as listener.
   *
   * @param model the virtual model
   * @param mapper the JSON mapper to serialize the events
   */
  public ConstraintEventBroadcaster(VirtualModel model, JsonMapper mapper) {
    this(model, mapper, HEARTBEAT_MILLIS);
  }

  /**
   * Creates a broadcaster with the given interval between keep-alive comments.
   *
   * @param model the virtual model
   * @param mapper the JSON mapper to serialize the events
   * @param heartbeatMillis the interval between keep-alive comments in milliseconds
   */
  ConstraintEventBroadcaster(VirtualModel model, JsonMapper mapper, long heartbeatMillis) {
    this.model = model;
    this.mapper = mapper;
    this.sender =
        Executors.newSingleThreadScheduledExecutor(
            runnable -> {
              Thread thread = new Thread(runnable, "vitruv-constraint-events");
              thread.setDaemon(true);
              return thread;
            });
    this.supported = register(model, this);
    sender.scheduleAtFixedRate(
        this::sendHeartbeat, heartbeatMillis, heartbeatMillis, TimeUnit.MILLISECONDS);
  }

  private static boolean register(VirtualModel model, ConstraintValidationListener listener) {
    try {
      model.addConstraintValidationListener(listener);
      return true;
    } catch (UnsupportedOperationException e) {
      LOGGER.warn("The virtual model does not report constraint validations: {}", e.getMessage());
      return false;
    }
  }

  /**
   * Subscribes a client. It first receives the current status of all constraints.
   *
   * @param stream the event stream of the client
   */
  public void subscribe(EventStream stream) {
    subscribers.add(stream);
    updateValidationAfterPropagation();
    // validate on the sender thread, the result is broadcast to all subscribers
    sender.execute(
        () -> {
          try {
            model.validateConstraints();
          } catch (RuntimeException e) {
            LOGGER.error("Validating the constraints for a new subscriber failed", e);
          }
        });
  }

  /**
   * Gets the number of subscribed clients.
   *
   * @return the number of subscribers
   */
  public int getSubscriberCount() {
    return subscribers.size();
  }

  @Override
  public void constraintsValidated(ConstraintValidationReport report, Trigger trigger) {
    if (subscribers.isEmpty()) {
      return;
    }
    // runs while the virtual model is locked: compute everything here, send later
    String revision = ConstraintRequests.revisionOf(model);
    String status = serialize(toStatus(report, revision));
    String constraints =
        trigger == Trigger.CONSTRAINTS_CHANGED
            ? serialize(new ConstraintRevisionDto(revision))
            : null;
    sender.execute(
        () -> {
          if (constraints != null) {
            broadcast(CONSTRAINTS_EVENT, constraints);
          }
          broadcast(VALIDATION_EVENT, status);
        });
  }

  private static ConstraintStatusDto toStatus(ConstraintValidationReport report, String revision) {
    Map<String, String> statuses = new LinkedHashMap<>();
    for (ConstraintValidationResult result : report.results()) {
      statuses.put(result.descriptor().id(), result.status().name());
    }
    return new ConstraintStatusDto(revision, report.isSatisfied(), statuses);
  }

  private String serialize(Object value) {
    try {
      return mapper.serialize(value);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Could not serialize a constraint event", e);
    }
  }

  private void broadcast(String event, String data) {
    String id = Long.toString(eventIds.incrementAndGet());
    for (EventStream subscriber : subscribers) {
      try {
        subscriber.send(event, id, data);
      } catch (IOException | RuntimeException e) {
        drop(subscriber);
      }
    }
  }

  private void sendHeartbeat() {
    // an exception would cancel all further heartbeats, so it is logged instead
    try {
      for (EventStream subscriber : subscribers) {
        try {
          subscriber.comment("heartbeat");
        } catch (IOException | RuntimeException e) {
          drop(subscriber);
        }
      }
    } catch (RuntimeException e) {
      LOGGER.error("Sending the keep-alive comments failed", e);
    }
  }

  private void drop(EventStream subscriber) {
    subscribers.remove(subscriber);
    subscriber.close();
    updateValidationAfterPropagation();
  }

  /**
   * Validates after propagation while clients are subscribed; restores the previous setting, e.g.
   * of the builder, when the last client left.
   */
  private synchronized void updateValidationAfterPropagation() {
    if (!supported) {
      return;
    }
    if (!subscribers.isEmpty() && settingWithoutSubscribers == null) {
      settingWithoutSubscribers = model.isConstraintValidationAfterPropagation();
      model.setConstraintValidationAfterPropagation(true);
    } else if (subscribers.isEmpty() && settingWithoutSubscribers != null) {
      model.setConstraintValidationAfterPropagation(settingWithoutSubscribers);
      settingWithoutSubscribers = null;
    }
  }

  /** Closes all event streams and stops sending. */
  @Override
  public void close() {
    sender.shutdownNow();
    for (EventStream subscriber : subscribers) {
      subscriber.close();
    }
    subscribers.clear();
    updateValidationAfterPropagation();
    if (supported) {
      model.removeConstraintValidationListener(this);
    }
  }
}
