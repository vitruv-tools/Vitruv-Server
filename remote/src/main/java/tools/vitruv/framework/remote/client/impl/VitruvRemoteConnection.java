package tools.vitruv.framework.remote.client.impl;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.eclipse.emf.ecore.resource.ResourceSet;
import tools.vitruv.change.atomic.root.InsertRootEObject;
import tools.vitruv.change.composite.description.VitruviusChange;
import tools.vitruv.change.utils.ProjectMarker;
import tools.vitruv.framework.remote.client.Subscription;
import tools.vitruv.framework.remote.client.VitruvClient;
import tools.vitruv.framework.remote.client.exception.BadClientResponseException;
import tools.vitruv.framework.remote.client.exception.BadServerResponseException;
import tools.vitruv.framework.remote.common.json.JsonFieldName;
import tools.vitruv.framework.remote.common.json.JsonMapper;
import tools.vitruv.framework.remote.common.rest.constants.ContentType;
import tools.vitruv.framework.remote.common.rest.constants.EndpointPath;
import tools.vitruv.framework.remote.common.rest.constants.Header;
import tools.vitruv.framework.remote.common.rest.constraints.ConstraintCreateRequest;
import tools.vitruv.framework.remote.common.rest.constraints.ConstraintDto;
import tools.vitruv.framework.remote.common.rest.constraints.ConstraintRevisionDto;
import tools.vitruv.framework.remote.common.rest.constraints.ConstraintStatusDto;
import tools.vitruv.framework.remote.common.rest.constraints.ConstraintUpdateRequest;
import tools.vitruv.framework.remote.common.rest.constraints.ConstraintValidationRequest;
import tools.vitruv.framework.remote.common.rest.constraints.ConstraintValidationResponse;
import tools.vitruv.framework.remote.common.rest.models.MetamodelDto;
import tools.vitruv.framework.remote.common.rest.models.ModelBatchRequest;
import tools.vitruv.framework.remote.common.rest.models.ModelDto;
import tools.vitruv.framework.remote.common.util.ResourceUtil;
import tools.vitruv.framework.views.ViewSelector;
import tools.vitruv.framework.views.ViewType;

/**
 * A {@link VitruvRemoteConnection} acts as a {@link HttpClient} to forward requests to a Vitruvius
 * server. This enables the ability to perform actions on this remote Vitruvius instance.
 */
public class VitruvRemoteConnection implements VitruvClient {
  /** Constant for successful request result. */
  private static final String SUCCESS = "success";

  /** Constant for exception during request processing. */
  private static final String EXCEPTION = "exception";

  /** The constant result. */
  private static final String RESULT = "result";

  /** The method constant. */
  private static final String METHOD = "method";

  /** The endpoint constant. */
  private static final String ENDPOINT = "endpoint";

  /** The metric client name. */
  private static final String METRIC_CLIENT_NAME = "vitruv.client.rest.client";

  private final int port;
  private final String hostOrIp;
  private final String protocol;
  private final HttpClient client;
  private final JsonMapper mapper;

  /**
   * Creates a new {@link VitruvRemoteConnection} using the given URL and port to connect to the
   * Vitruvius server.
   *
   * @param protocol The protocol of the Vitruvius server.
   * @param hostOrIp The host name of IP address of the Vitruvius server.
   * @param port of the Vitruvius server.
   */
  public VitruvRemoteConnection(String protocol, String hostOrIp, int port, Path temp) {
    this.client = HttpClient.newHttpClient();
    this.protocol = protocol;
    this.hostOrIp = hostOrIp;
    this.port = port;

    try {
      if (Files.notExists(temp) || (Files.isDirectory(temp) && isDirectoryEmpty(temp))) {
        Files.createDirectories(temp);
        ProjectMarker.markAsProjectRootFolder(temp);
      }
    } catch (IOException e) {
      throw new IllegalArgumentException(
          "Given temporary directory for models could not be created!", e);
    }

    this.mapper = new JsonMapper(temp);
  }

  private boolean isDirectoryEmpty(Path directory) throws IOException {
    try (Stream<Path> entries = Files.list(directory)) {
      return entries.findAny().isEmpty();
    }
  }

  @Override
  public List<ConstraintDto> getConstraints() {
    var request =
        HttpRequest.newBuilder().uri(createURIFrom(EndpointPath.CONSTRAINTS)).GET().build();
    try {
      var response = sendRequest(request);
      return mapper.deserializeArrayOf(response.body(), ConstraintDto.class);
    } catch (IOException e) {
      throw new BadClientResponseException(e);
    }
  }

  @Override
  public Optional<ConstraintDto> getConstraint(String id) {
    Objects.requireNonNull(id, "id");
    var request =
        HttpRequest.newBuilder().uri(createURIFrom(EndpointPath.constraint(id))).GET().build();
    try {
      var response = sendRequest(request);
      return Optional.of(mapper.deserialize(response.body(), ConstraintDto.class));
    } catch (BadServerResponseException e) {
      if (e.getStatusCode() == HttpURLConnection.HTTP_NOT_FOUND) {
        return Optional.empty();
      }
      throw e;
    } catch (IOException e) {
      throw new BadClientResponseException(e);
    }
  }

  @Override
  public ConstraintDto createConstraint(String language, String source) {
    try {
      var request =
          HttpRequest.newBuilder()
              .uri(createURIFrom(EndpointPath.CONSTRAINTS))
              .header(Header.CONTENT_TYPE, ContentType.APPLICATION_JSON)
              .POST(
                  BodyPublishers.ofString(
                      mapper.serialize(new ConstraintCreateRequest(language, source))))
              .build();
      return mapper.deserialize(sendRequest(request).body(), ConstraintDto.class);
    } catch (IOException e) {
      throw new BadClientResponseException(e);
    }
  }

  @Override
  public ConstraintDto updateConstraint(String id, String source) {
    Objects.requireNonNull(id, "id");
    try {
      var request =
          HttpRequest.newBuilder()
              .uri(createURIFrom(EndpointPath.constraint(id)))
              .header(Header.CONTENT_TYPE, ContentType.APPLICATION_JSON)
              .PUT(BodyPublishers.ofString(mapper.serialize(new ConstraintUpdateRequest(source))))
              .build();
      return mapper.deserialize(sendRequest(request).body(), ConstraintDto.class);
    } catch (IOException e) {
      throw new BadClientResponseException(e);
    }
  }

  @Override
  public void deleteConstraint(String id) {
    Objects.requireNonNull(id, "id");
    var request =
        HttpRequest.newBuilder().uri(createURIFrom(EndpointPath.constraint(id))).DELETE().build();
    sendRequest(request);
  }

  @Override
  public String getConstraintRevision() {
    var request =
        HttpRequest.newBuilder()
            .uri(createURIFrom(EndpointPath.CONSTRAINTS_REVISION))
            .GET()
            .build();
    try {
      return mapper
          .deserialize(sendRequest(request).body(), ConstraintRevisionDto.class)
          .revision();
    } catch (IOException e) {
      throw new BadClientResponseException(e);
    }
  }

  @Override
  public ConstraintValidationResponse validateConstraints(ConstraintValidationRequest request) {
    Objects.requireNonNull(request, "request");
    try {
      var httpRequest =
          HttpRequest.newBuilder()
              .uri(createURIFrom(EndpointPath.CONSTRAINTS_VALIDATE))
              .header(Header.CONTENT_TYPE, ContentType.APPLICATION_JSON)
              .POST(BodyPublishers.ofString(mapper.serialize(request)))
              .build();
      var response = sendRequest(httpRequest);
      return mapper.deserialize(response.body(), ConstraintValidationResponse.class);
    } catch (IOException e) {
      throw new BadClientResponseException(e);
    }
  }

  @Override
  public ConstraintStatusDto getConstraintStatus(Collection<String> constraintIds) {
    if (constraintIds != null && constraintIds.isEmpty()) {
      // an empty selection validates nothing, as for validateConstraints
      return new ConstraintStatusDto(getConstraintRevision(), true, Map.of());
    }
    String path = EndpointPath.CONSTRAINTS_STATUS;
    if (constraintIds != null) {
      StringJoiner query = new StringJoiner("&", "?", "");
      for (String id : constraintIds) {
        query.add("id=" + URLEncoder.encode(id, StandardCharsets.UTF_8).replace("+", "%20"));
      }
      path += query.toString();
    }
    var request = HttpRequest.newBuilder().uri(createURIFrom(path)).GET().build();
    try {
      return mapper.deserialize(sendRequest(request).body(), ConstraintStatusDto.class);
    } catch (IOException e) {
      throw new BadClientResponseException(e);
    }
  }

  @Override
  public Subscription subscribeConstraintEvents(Consumer<ConstraintStatusDto> listener) {
    Objects.requireNonNull(listener, "listener");
    var request =
        HttpRequest.newBuilder()
            .uri(createURIFrom(EndpointPath.CONSTRAINTS_EVENTS))
            .header(Header.ACCEPT, ContentType.TEXT_EVENT_STREAM)
            .GET()
            .build();
    HttpResponse<InputStream> response;
    try {
      response = client.send(request, BodyHandlers.ofInputStream());
    } catch (IOException e) {
      throw new BadServerResponseException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new BadServerResponseException(e);
    }
    if (response.statusCode() != HttpURLConnection.HTTP_OK) {
      try (InputStream body = response.body()) {
        throw new BadServerResponseException(
            new String(body.readAllBytes(), StandardCharsets.UTF_8), response.statusCode());
      } catch (IOException e) {
        throw new BadServerResponseException(e);
      }
    }
    return ConstraintEventSubscription.start(response.body(), mapper, listener);
  }

  @Override
  public List<ModelDto> getModelDescriptors() {
    var request = HttpRequest.newBuilder().uri(createURIFrom(EndpointPath.MODELS)).GET().build();
    try {
      return mapper.deserializeArrayOf(sendRequest(request).body(), ModelDto.class);
    } catch (IOException e) {
      throw new BadClientResponseException(e);
    }
  }

  @Override
  public ResourceSet getModelContents(Collection<String> ids) {
    try {
      var request =
          HttpRequest.newBuilder()
              .uri(createURIFrom(EndpointPath.MODELS_BATCH))
              .header(Header.CONTENT_TYPE, ContentType.APPLICATION_JSON)
              .POST(
                  BodyPublishers.ofString(
                      mapper.serialize(
                          new ModelBatchRequest(ids == null ? null : List.copyOf(ids)))))
              .build();
      return mapper.deserialize(sendRequest(request).body(), ResourceSet.class);
    } catch (IOException e) {
      throw new BadClientResponseException(e);
    }
  }

  @Override
  public String getModelXmi(String id) {
    Objects.requireNonNull(id, "id");
    return getXmi(EndpointPath.model(id));
  }

  @Override
  public List<MetamodelDto> getMetamodelDescriptors() {
    var request =
        HttpRequest.newBuilder().uri(createURIFrom(EndpointPath.METAMODELS)).GET().build();
    try {
      return mapper.deserializeArrayOf(sendRequest(request).body(), MetamodelDto.class);
    } catch (IOException e) {
      throw new BadClientResponseException(e);
    }
  }

  @Override
  public String getMetamodelXmi(String nsUri) {
    Objects.requireNonNull(nsUri, "nsUri");
    return getXmi(EndpointPath.metamodel(nsUri));
  }

  private String getXmi(String path) {
    var request =
        HttpRequest.newBuilder()
            .uri(createURIFrom(path))
            .header(Header.ACCEPT, ContentType.APPLICATION_XMI)
            .GET()
            .build();
    return sendRequest(request).body();
  }

  /**
   * Returns a list of remote representations of {@link ViewType}s available at the Vitruvius
   * server.
   */
  @Override
  public Collection<ViewType<?>> getViewTypes() {
    var request =
        HttpRequest.newBuilder().uri(createURIFrom(EndpointPath.VIEW_TYPES)).GET().build();
    try {
      var response = sendRequest(request);
      var typeNames = mapper.deserializeArrayOf(response.body(), String.class);
      var list = new LinkedList<ViewType<?>>();
      typeNames.forEach(it -> list.add(new RemoteViewType(it, this)));
      return list;
    } catch (IOException e) {
      throw new BadClientResponseException(e);
    }
  }

  /**
   * Returns a view selector for the given {@link ViewType} by querying the selector from the
   * Vitruvius server. The view type must be of type {@link RemoteViewType} as these represent the
   * actual view types available at the server side.
   *
   * @param viewType The {@link ViewType} to create a selector for.
   * @return A {@link ViewSelector} for the given view type.
   * @throws IllegalArgumentException If view type is no {@link RemoteViewType}.
   */
  @Override
  public <S extends ViewSelector> S createSelector(ViewType<S> viewType) {
    if (!(viewType instanceof RemoteViewType)) {
      throw new IllegalArgumentException("This vitruv client can only process RemoteViewType!");
    }
    return viewType.createSelector(null);
  }

  /**
   * Queries the Vitruvius server to obtain a view selector for the given view type.
   *
   * @param viewType The view type to obtain a selector for.
   * @return The selector generated with the given view type; it remembers the view type so that
   *     the views it creates can report it.
   * @throws BadServerResponseException If the server answered with a bad response or a connection
   *     error occurred.
   * @throws NoSuchElementException If the response headers do not contain the expected selector
   *     UUID.
   */
  RemoteViewSelector getSelector(RemoteViewType viewType) throws BadServerResponseException {
    var request =
        HttpRequest.newBuilder()
            .uri(createURIFrom(EndpointPath.VIEW_SELECTOR))
            .header(Header.VIEW_TYPE, viewType.getName())
            .GET()
            .build();
    try {
      var response = sendRequest(request);
      var resource =
          mapper.deserializeResource(
              response.body(), JsonFieldName.TEMP_VALUE, ResourceUtil.createJsonResourceSet());
      Optional<String> selectorUuid = response.headers().firstValue(Header.SELECTOR_UUID);
      if (selectorUuid.isPresent()) {
        return new RemoteViewSelector(selectorUuid.get(), resource, viewType, this);
      } else {
        // Handle the case where the value is not present
        throw new NoSuchElementException("Header.SELECTOR_UUID not found in response headers");
      }
    } catch (IOException e) {
      throw new BadClientResponseException(e);
    }
  }

  /**
   * Queries the Vitruvius server to obtain the view using the given view selector.
   *
   * @param selector The {@link tools.vitruv.framework.views.ViewSelector} which should be used to
   *     create the view.
   * @return The view generated with the given view selector.
   * @throws BadServerResponseException If the server answered with a bad response or a connection
   *     error occurred.
   */
  RemoteView getView(RemoteViewSelector selector) throws BadServerResponseException {
    try {
      var request =
          HttpRequest.newBuilder()
              .uri(createURIFrom(EndpointPath.VIEW))
              .header(Header.SELECTOR_UUID, selector.getUUID())
              .POST(BodyPublishers.ofString(mapper.serialize(selector.getSelectionIds())))
              .build();
      var response = sendRequest(request);
      var rSet = mapper.deserialize(response.body(), ResourceSet.class);
      Optional<String> viewUuid = response.headers().firstValue(Header.VIEW_UUID);
      if (viewUuid.isPresent()) {
        return new RemoteView(viewUuid.get(), rSet, selector, selector.getViewType(), this);
      } else {
        // Handle the case where the value is not present
        throw new NoSuchElementException("Header.VIEW_UUID not found in response headers");
      }
    } catch (IOException e) {
      throw new BadClientResponseException(e);
    }
  }

  /**
   * Queries the Vitruvius server to propagate the given changes for the view with the given UUID.
   *
   * @param uuid UUID of the changed view.
   * @param change The changes performed on the affected view.
   * @throws BadServerResponseException If the server answered with a bad response or a connection
   *     error occurred.
   */
  void propagateChanges(String uuid, VitruviusChange<?> change) throws BadServerResponseException {
    try {
      change
          .getEChanges()
          .forEach(
              it -> {
                if (it instanceof InsertRootEObject<?>) {
                  ((InsertRootEObject<?>) it).setResource(null);
                }
              });
      var jsonBody = mapper.serialize(change);
      var request =
          HttpRequest.newBuilder()
              .uri(createURIFrom(EndpointPath.VIEW))
              .header(Header.CONTENT_TYPE, ContentType.APPLICATION_JSON)
              .header(Header.VIEW_UUID, uuid)
              .method("PATCH", BodyPublishers.ofString(jsonBody))
              .build();
      sendRequest(request);
    } catch (IOException e) {
      throw new BadClientResponseException(e);
    }
  }

  /**
   * Queries the Vitruvius server to close the view with the given.
   *
   * @param uuid UUID of the view.
   * @throws BadServerResponseException If the server answered with a bad response or a connection
   *     error occurred.
   */
  void closeView(String uuid) throws BadServerResponseException {
    var request =
        HttpRequest.newBuilder()
            .uri(createURIFrom(EndpointPath.VIEW))
            .header(Header.VIEW_UUID, uuid)
            .DELETE()
            .build();
    sendRequest(request);
  }

  /**
   * Queries the Vitruvius serve to check if the view with the given ID is closed.
   *
   * @param uuid UUID of the view.
   * @return {@code true} if the view is closed, {@code false} otherwise.
   * @throws BadServerResponseException If the server answered with a bad response or a connection
   *     error occurred.
   */
  boolean isViewClosed(String uuid) throws BadServerResponseException {
    var request =
        HttpRequest.newBuilder()
            .uri(createURIFrom(EndpointPath.IS_VIEW_CLOSED))
            .header(Header.VIEW_UUID, uuid)
            .GET()
            .build();
    return sendRequestAndCheckBooleanResult(request);
  }

  /**
   * Queries the Vitruvius server to check if the view with the given ID is outdated.
   *
   * @param uuid UUID of the view.
   * @return {@code true} if the view is outdated, {@code false} otherwise.
   */
  boolean isViewOutdated(String uuid) {
    var request =
        HttpRequest.newBuilder()
            .uri(createURIFrom(EndpointPath.IS_VIEW_OUTDATED))
            .header(Header.VIEW_UUID, uuid)
            .GET()
            .build();
    return sendRequestAndCheckBooleanResult(request);
  }

  /**
   * Queries the Vitruvius server to update the view with the given ID.
   *
   * @param uuid UUID of the view.
   * @return The updated {@link ResourceSet} of the view.
   * @throws BadServerResponseException If the server answered with a bad response or a connection
   *     error occurred.
   */
  ResourceSet updateView(String uuid) throws BadServerResponseException {
    var request =
        HttpRequest.newBuilder()
            .uri(createURIFrom(EndpointPath.VIEW))
            .header(Header.VIEW_UUID, uuid)
            .GET()
            .build();
    try {
      var response = sendRequest(request);
      return mapper.deserialize(response.body(), ResourceSet.class);
    } catch (IOException e) {
      throw new BadClientResponseException(e);
    }
  }

  private boolean sendRequestAndCheckBooleanResult(HttpRequest request) {
    var response = sendRequest(request);
    if (!Objects.equals(response.body(), Boolean.TRUE.toString())
        && !Objects.equals(response.body(), Boolean.FALSE.toString())) {
      throw new BadServerResponseException(
          "Expected response to be true or false! Actual: " + response);
    }
    return response.body().equals(Boolean.TRUE.toString());
  }

  private HttpResponse<String> sendRequest(HttpRequest request) {
    var timer = Timer.start(Metrics.globalRegistry);
    try {
      var response = client.send(request, BodyHandlers.ofString());
      if (response.statusCode() / 100 != 2) {
        timer.stop(
            Metrics.timer(
                METRIC_CLIENT_NAME,
                ENDPOINT,
                request.uri().getPath(),
                METHOD,
                request.method(),
                RESULT,
                "" + response.statusCode()));
        throw new BadServerResponseException(response.body(), response.statusCode());
      }
      timer.stop(
          Metrics.timer(
              METRIC_CLIENT_NAME,
              ENDPOINT,
              request.uri().getPath(),
              METHOD,
              request.method(),
              RESULT,
              SUCCESS));
      return response;
    } catch (IOException e) {
      timer.stop(
          Metrics.timer(
              METRIC_CLIENT_NAME,
              ENDPOINT,
              request.uri().getPath(),
              METHOD,
              request.method(),
              RESULT,
              EXCEPTION));
      throw new BadServerResponseException(e);
    } catch (InterruptedException e) {
      timer.stop(
          Metrics.timer(
              METRIC_CLIENT_NAME,
              ENDPOINT,
              request.uri().getPath(),
              METHOD,
              request.method(),
              RESULT,
              EXCEPTION));
      Thread.currentThread().interrupt(); // Re-interrupt the current thread
      throw new BadServerResponseException(e);
    }
  }

  private URI createURIFrom(String path) {
    return URI.create(String.format("%s://%s:%d%s", protocol, hostOrIp, port, path));
  }
}
