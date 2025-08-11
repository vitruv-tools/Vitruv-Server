package tools.vitruv.framework.remote.client.impl;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.eclipse.emf.ecore.resource.ResourceSet;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import tools.vitruv.change.atomic.root.InsertRootEObject;
import tools.vitruv.change.composite.description.VitruviusChange;
import tools.vitruv.change.utils.ProjectMarker;
import tools.vitruv.framework.remote.client.VitruvClient;
import tools.vitruv.framework.remote.client.exception.BadClientResponseException;
import tools.vitruv.framework.remote.client.exception.BadServerResponseException;
import tools.vitruv.framework.remote.client.http.VitruvHttpClientWrapper;
import tools.vitruv.framework.remote.client.http.VitruvHttpRequest;
import tools.vitruv.framework.remote.client.http.VitruvHttpResponseWrapper;
import tools.vitruv.framework.remote.common.json.JsonFieldName;
import tools.vitruv.framework.remote.common.json.JsonMapper;
import tools.vitruv.framework.remote.common.rest.constants.ContentType;
import tools.vitruv.framework.remote.common.rest.constants.EndpointPath;
import tools.vitruv.framework.remote.common.rest.constants.Header;
import tools.vitruv.framework.remote.common.util.ResourceUtil;
import tools.vitruv.framework.views.ViewSelector;
import tools.vitruv.framework.views.ViewType;

/**
 * A {@link VitruvRemoteConnection} acts as a {@link HttpClient} to forward requests to a Vitruvius
 * server. This enables the ability to perform actions on this remote Vitruvius instance.
 */
public class VitruvRemoteConnection implements VitruvClient {
    private static final String SUCCESS = "success";
    private static final String EXCEPTION = "exception";
    private static final String RESULT = "result";
    private static final String METHOD = "method";
    private static final String ENDPOINT = "endpoint";
    private static final String METRIC_CLIENT_NAME = "vitruv.client.rest.client";
    private final String baseUri;
    private final VitruvHttpClientWrapper client;
    private final JsonMapper mapper;

    /**
     * Creates a new {@link VitruvRemoteConnection} using a URI, client, and directory.
     *
     * @param baseUri Base URI of the Vitruvius server against which requests are sent.
     * @param client Wrapper for the actual HTTP client.
     * @param temp A temporary directory, in which temporary files and data can be stored.
     */
    public VitruvRemoteConnection(String baseUri, VitruvHttpClientWrapper client, Path temp) {
        this.baseUri = baseUri;
        this.client = client;

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

    /**
     * Returns a list of remote representations of {@link ViewType}s available at the Vitruvius
     * server.
     */
    @Override
    public Collection<ViewType<?>> getViewTypes() {
        var request = VitruvHttpRequest.GET(createURIFrom(EndpointPath.VIEW_TYPES));
        try {
            var response = sendRequest(request);
            var typeNames = mapper.deserializeArrayOf(response.getBody().orElse(""), String.class);
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
            throw new IllegalArgumentException(
                    "This vitruv client can only process RemoteViewType!");
        }
        return viewType.createSelector(null);
    }

    /**
     * Queries the Vitruvius server to obtain a view selector from the view type with the given
     * name.
     *
     * @param typeName The name of the view type.
     * @return The selector generated with the view type of the given name.
     * @throws BadServerResponseException If the server answered with a bad response or a connection
     *         error occurred.
     * @throws NoSuchElementException If the response headers do not contain the expected selector
     *         UUID.
     */
    RemoteViewSelector getSelector(String typeName) throws BadServerResponseException {
        var request = VitruvHttpRequest.GET(createURIFrom(EndpointPath.VIEW_SELECTOR))
                .addHeader(Header.VIEW_TYPE, typeName);
        try {
            var response = sendRequest(request);
            var resource = mapper.deserializeResource(response.getBody().orElse(""), JsonFieldName.TEMP_VALUE,
                    ResourceUtil.createJsonResourceSet());
            Optional<String> selectorUuid = response.getHeader(Header.SELECTOR_UUID);
            if (selectorUuid.isPresent()) {
                return new RemoteViewSelector(selectorUuid.get(), resource, this);
            } else {
                // Handle the case where the value is not present
                throw new NoSuchElementException(
                        "Header.SELECTOR_UUID not found in response headers");
            }
        } catch (IOException e) {
            throw new BadClientResponseException(e);
        }
    }

    /**
     * Queries the Vitruvius server to obtain the view using the given view selector.
     *
     * @param selector The {@link tools.vitruv.framework.views.ViewSelector} which should be used to
     *        create the view.
     * @return The view generated with the given view selector.
     * @throws BadServerResponseException If the server answered with a bad response or a connection
     *         error occurred.
     */
    RemoteView getView(RemoteViewSelector selector) throws BadServerResponseException {
        try {
            var request = VitruvHttpRequest.POST(createURIFrom(EndpointPath.VIEW))
                    .addHeader(Header.SELECTOR_UUID, selector.getUUID())
                    .setBody(mapper.serialize(selector.getSelectionIds()));
            var response = sendRequest(request);
            var rSet = mapper.deserialize(response.getBody().orElse(""), ResourceSet.class);
            Optional<String> viewUuid = response.getHeader(Header.VIEW_UUID);
            if (viewUuid.isPresent()) {
                return new RemoteView(viewUuid.get(), rSet, selector, this);
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
     *         error occurred.
     */
    void propagateChanges(String uuid, VitruviusChange<?> change)
            throws BadServerResponseException {
        try {
            change.getEChanges().forEach(it -> {
                if (it instanceof InsertRootEObject<?>) {
                    ((InsertRootEObject<?>) it).setResource(null);
                }
            });
            var jsonBody = mapper.serialize(change);
            var request = VitruvHttpRequest.PATCH(createURIFrom(EndpointPath.VIEW))
                    .addHeader(Header.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                    .addHeader(Header.VIEW_UUID, uuid)
                    .setBody(jsonBody);
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
     *         error occurred.
     */
    void closeView(String uuid) throws BadServerResponseException {
        var request = VitruvHttpRequest.DELETE(createURIFrom(EndpointPath.VIEW))
                .addHeader(Header.VIEW_UUID, uuid);
        sendRequest(request);
    }

    /**
     * Queries the Vitruvius serve to check if the view with the given ID is closed.
     *
     * @param uuid UUID of the view.
     * @return {@code true} if the view is closed, {@code false} otherwise.
     * @throws BadServerResponseException If the server answered with a bad response or a connection
     *         error occurred.
     */
    boolean isViewClosed(String uuid) throws BadServerResponseException {
        var request = VitruvHttpRequest.GET(createURIFrom(EndpointPath.IS_VIEW_CLOSED))
                .addHeader(Header.VIEW_UUID, uuid);
        return sendRequestAndCheckBooleanResult(request);
    }

    /**
     * Queries the Vitruvius server to check if the view with the given ID is outdated.
     *
     * @param uuid UUID of the view.
     * @return {@code true} if the view is outdated, {@code false} otherwise.
     */
    boolean isViewOutdated(String uuid) {
        var request = VitruvHttpRequest.GET(createURIFrom(EndpointPath.IS_VIEW_OUTDATED))
                .addHeader(Header.VIEW_UUID, uuid);
        return sendRequestAndCheckBooleanResult(request);
    }

    /**
     * Queries the Vitruvius server to update the view with the given ID.
     *
     * @param uuid UUID of the view.
     * @return The updated {@link ResourceSet} of the view.
     * @throws BadServerResponseException If the server answered with a bad response or a connection
     *         error occurred.
     */
    ResourceSet updateView(String uuid) throws BadServerResponseException {
        var request = VitruvHttpRequest.GET(createURIFrom(EndpointPath.VIEW))
                .addHeader(Header.VIEW_UUID, uuid);
        try {
            var response = sendRequest(request);
            return mapper.deserialize(response.getBody().orElse(""), ResourceSet.class);
        } catch (IOException e) {
            throw new BadClientResponseException(e);
        }
    }

    private boolean sendRequestAndCheckBooleanResult(VitruvHttpRequest request) {
        var response = sendRequest(request);
        if (!Objects.equals(response.getBody().orElse(""), Boolean.TRUE.toString())
                && !Objects.equals(response.getBody().orElse(""), Boolean.FALSE.toString())) {
            throw new BadServerResponseException(
                    "Expected response to be true or false! Actual: " + response);
        }
        return response.getBody().orElse("").equals(Boolean.TRUE.toString());
    }

    private VitruvHttpResponseWrapper sendRequest(VitruvHttpRequest request) {
        var timer = Timer.start(Metrics.globalRegistry);
        try {
            var response = request.sendRequest(this.client);
            if (response.getStatusCode() != HttpURLConnection.HTTP_OK) {
                timer.stop(Metrics.timer(METRIC_CLIENT_NAME, ENDPOINT, request.getUri(),
                        METHOD, request.getMethod(), RESULT, "" + response.getStatusCode()));
                throw new BadServerResponseException(response.getBody().orElse(""), response.getStatusCode());
            }
            timer.stop(Metrics.timer(METRIC_CLIENT_NAME, ENDPOINT, request.getUri(),
                    METHOD, request.getMethod(), RESULT, SUCCESS));
            return response;
        } catch (Exception e) {
            timer.stop(Metrics.timer(METRIC_CLIENT_NAME, ENDPOINT, request.getUri(),
                    METHOD, request.getMethod(), RESULT, EXCEPTION));
            throw new BadServerResponseException(e);
        }
    }

    private String createURIFrom(String path) {
        return String.format("%s%s", this.baseUri, path);
    }
}
