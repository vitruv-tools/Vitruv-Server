package tools.vitruv.framework.remote.server.rest.endpoints;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_CONFLICT;
import static java.net.HttpURLConnection.HTTP_UNAVAILABLE;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import java.io.IOException;
import java.util.Arrays;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import tools.vitruv.change.atomic.hid.HierarchicalId;
import tools.vitruv.change.atomic.root.InsertRootEObject;
import tools.vitruv.change.composite.description.VitruviusChange;
import tools.vitruv.framework.remote.common.json.JsonMapper;
import tools.vitruv.framework.remote.common.rest.TaskStatus;
import tools.vitruv.framework.remote.common.rest.constants.EndpointPath;
import tools.vitruv.framework.remote.common.rest.constants.Header;
import tools.vitruv.framework.remote.server.exception.ServerHaltingException;
import tools.vitruv.framework.remote.server.http.HttpWrapper;
import tools.vitruv.framework.remote.server.propagation.PropagationWorker;
import tools.vitruv.framework.remote.server.rest.PatchEndpoint;
import tools.vitruv.framework.views.impl.ModifiableView;
import tools.vitruv.framework.views.impl.ViewCreatingViewType;

/**
 * This endpoint applies given {@link VitruviusChange}s to the VSUM.
 *
 * <p>By default, the changes are propagated before the endpoint answers, once the changes the view
 * sent for asynchronous propagation before have been applied. If the request carries the header
 * {@code Prefer: respond-async}, the endpoint answers right away with 202 (Accepted), the task id
 * in the {@link Header#TASK_ID} header and the {@link TaskStatus} as body. The {@link
 * PropagationWorker} then propagates the changes in the background, and the client follows them at
 * {@link EndpointPath#TASK}.
 */
public class ChangePropagationEndpoint implements PatchEndpoint {
  private static final String ENDPOINT_METRIC_NAME = "vitruv.server.rest.propagation";
  private final JsonMapper mapper;
  private final Object vsumLock;
  private final PropagationWorker propagationWorker;

  /**
   * Creates a new ChangePropagationEndpoint.
   *
   * @param mapper The JSON mapper to use.
   * @param vsumLock The lock to hold while propagating changes before answering.
   * @param propagationWorker The worker that propagates changes in the background.
   */
  public ChangePropagationEndpoint(
      JsonMapper mapper, Object vsumLock, PropagationWorker propagationWorker) {
    this.mapper = mapper;
    this.vsumLock = vsumLock;
    this.propagationWorker = propagationWorker;
  }

  @SuppressWarnings("unchecked")
  @Override
  public String process(HttpWrapper wrapper) {
    var view = Cache.getView(wrapper.getRequestHeader(Header.VIEW_UUID));
    if (view == null) {
      throw notFound("View with given id not found!");
    }
    var change = readChange(wrapper);
    var type = (ViewCreatingViewType<?, HierarchicalId>) view.getViewType();

    if (prefersAsync(wrapper.getRequestHeader(Header.PREFER))) {
      // Neither the submission nor the status needs the VSUM lock, so the client gets its answer
      // right away, also while the worker is propagating another change.
      int taskId = propagationWorker.submit((ModifiableView) view, type, change);
      wrapper.setResponseCode(HTTP_ACCEPTED);
      wrapper.addResponseHeader(Header.PREFERENCE_APPLIED, Header.RESPOND_ASYNC);
      wrapper.addResponseHeader(Header.TASK_ID, Integer.toString(taskId));
      return TaskEndpointSupport.respondWith(
          propagationWorker.getStatus(taskId).orElseThrow(), wrapper, mapper);
    }

    // The change was recorded after the changes the view sent for asynchronous propagation before,
    // so it can only be resolved once they are in the VSUM.
    try {
      propagationWorker.awaitAppliedSubmissions((ModifiableView) view);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ServerHaltingException(HTTP_UNAVAILABLE, "Interrupted while waiting: " + e);
    } catch (IllegalStateException e) {
      throw new ServerHaltingException(HTTP_UNAVAILABLE, e.getMessage());
    }
    synchronized (vsumLock) {
      var propTimer = Timer.start(Metrics.globalRegistry);
      try {
        type.commitViewChanges((ModifiableView) view, change);
        propTimer.stop(Metrics.timer(ENDPOINT_METRIC_NAME, "propagation", "success"));
      } catch (RuntimeException e) {
        propTimer.stop(Metrics.timer(ENDPOINT_METRIC_NAME, "propagation", "failure"));
        throw new ServerHaltingException(HTTP_CONFLICT, "Changes rejected: " + e.getMessage());
      }
    }
    return null;
  }

  /**
   * Returns whether the value of a Prefer header contains the respond-async preference. Following
   * RFC 7240, preferences are separated by commas, may carry a value or parameters, and their names
   * are compared ignoring case.
   *
   * @param preferHeader the value of the header, may be {@code null}
   * @return {@code true} if the client asks for an asynchronous answer
   */
  static boolean prefersAsync(String preferHeader) {
    if (preferHeader == null) {
      return false;
    }
    return Arrays.stream(preferHeader.split(","))
        .map(preference -> preference.split("[=;]", 2)[0].trim())
        .anyMatch(Header.RESPOND_ASYNC::equalsIgnoreCase);
  }

  @SuppressWarnings("unchecked")
  private VitruviusChange<HierarchicalId> readChange(HttpWrapper wrapper) {
    String body;
    try {
      body = wrapper.getRequestBodyAsString();
    } catch (IOException e) {
      throw internalServerError(e.getMessage());
    }

    VitruviusChange<HierarchicalId> change;
    var desTimer = Timer.start(Metrics.globalRegistry);
    try {
      change = mapper.deserialize(body, VitruviusChange.class);
      desTimer.stop(Metrics.timer(ENDPOINT_METRIC_NAME, "deserialization", "success"));
    } catch (JsonProcessingException e) {
      desTimer.stop(Metrics.timer(ENDPOINT_METRIC_NAME, "deserialization", "failure"));
      throw new ServerHaltingException(HTTP_BAD_REQUEST, e.getMessage());
    }
    change
        .getEChanges()
        .forEach(
            it -> {
              if (it instanceof InsertRootEObject<?> echange) {
                echange.setResource(new ResourceImpl(URI.createURI(echange.getUri())));
              }
            });
    return change;
  }
}
