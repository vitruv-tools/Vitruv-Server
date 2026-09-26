package tools.vitruv.framework.remote.modules.vsums.usecasesimpls;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.stereotype.Component;
import tools.vitruv.framework.remote.config.VitruvServerProperties;
import tools.vitruv.framework.remote.modules.vsums.usecases.dtos.OpenViewRequestBody;
import tools.vitruv.framework.remote.modules.vsums.usecases.strategies.ViewUseCasesStrategy;
import tools.vitruv.framework.remote.modules.vsums.usecases.strategies.VsumUseCasesStrategy;
import tools.vitruv.framework.remote.modules.vsums.usecasesimpls.helpers.VitruvServerIdsManager;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

/**
 * Implementation of the strategy interfaces {@link VsumUseCasesStrategy} and {@link ViewUseCasesStrategy}
 * for interacting with the remote Vitruv server.
 * Handles operations such as querying view types, creating selectors, opening views, making updates,
 * and committing changes to a view resource set.
 * <p>
 * This class communicates with the Vitruv server via HTTP requests and processes the responses.
 * It leverages {@link VitruvServerProperties} for configuration and {@link VitruvServerIdsManager} for
 * managing identifiers of entities created on the server.
 */
@Component
@RequiredArgsConstructor
public class VitruvServerStrategyImpl implements VsumUseCasesStrategy, ViewUseCasesStrategy {
  private final VitruvServerProperties vitruvServerProperties;
  private final VitruvServerIdsManager vitruvServerIdsManager;

  private final HttpClient client = HttpClient.newHttpClient();
  private final ObjectMapper mapper = new ObjectMapper();

  /**
   * Checks the availability of the remote Vitruv server.
   * Sends a health check request to the server's health endpoint and determines
   * if the server is available.
   *
   * @return true if the server is available and responds without errors, false otherwise
   */
  public boolean isAvailable() {
    val request = HttpRequest.newBuilder()
        .uri(URI.create(vitruvServerProperties.url() + "/health"))
        .GET()
        .build();

    try {
      sendRequest(request);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String[] getViewTypes(UUID vsumId) {
    val request = HttpRequest.newBuilder()
        .uri(URI.create(vitruvServerProperties.url() + "/vsum/view/types"))
        .GET()
        .build();

    val response = sendRequest(request);
    String[] viewTypes;

    try {
      viewTypes = mapper.readValue(response.body(), String[].class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }

    return viewTypes;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void deleteVsum(UUID vsumId) {
    throw new UnsupportedOperationException("Vitruv-Server does not support deleting Vsums");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CreateSelectorResponse createSelector(UUID vsumId, String viewTypeName) {
    val request = HttpRequest.newBuilder()
        .uri(URI.create(vitruvServerProperties.url() + "/vsum/view/selector"))
        .header("view-type", viewTypeName)
        .GET()
        .build();

    val response = sendRequest(request);
    val selectorId = UUID.fromString(
        response.headers().firstValue("selector-uuid").orElseThrow()
    );
    vitruvServerIdsManager.addId(selectorId);
    String responseBody = response.body();

    return new CreateSelectorResponse(selectorId, responseBody);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OpenViewResponse openView(OpenViewRequestBody body) {
    final String requestBody;
    try {
      requestBody = mapper.writeValueAsString(body.selectedObjectIds());
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }

    val request = HttpRequest.newBuilder()
        .uri(URI.create(vitruvServerProperties.url() + "/vsum/view"))
        .header("Content-Type", "application/json")
        .header("selector-uuid", body.selectorId().toString())
        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
        .build();

    val response = sendRequest(request);
    val viewId = UUID.fromString(
        response.headers().firstValue("view-uuid").orElseThrow()
    );
    vitruvServerIdsManager.addId(viewId);
    String responseBody = response.body();

    return new OpenViewResponse(viewId, responseBody);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String commitResourceSet(UUID viewId, String resourceSetBody) {
    val request = HttpRequest.newBuilder()
        .uri(URI.create(vitruvServerProperties.url() + "/vsum/view/derive-changes"))
        .header("Content-Type", "application/json")
        .header("view-uuid", viewId.toString())
        .method("PATCH", HttpRequest.BodyPublishers.ofString(resourceSetBody))
        .build();
    val response = sendRequest(request);
    return response.body();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String update(UUID viewId) {
    val request = HttpRequest.newBuilder()
        .uri(URI.create(vitruvServerProperties.url() + "/vsum/view"))
        .header("view-uuid", viewId.toString())
        .GET()
        .build();
    val response = sendRequest(request);
    return response.body();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void closeView(UUID viewId) {
    val request = HttpRequest.newBuilder()
        .uri(URI.create(vitruvServerProperties.url() + "/vsum/view"))
        .header("view-uuid", viewId.toString())
        .DELETE()
        .build();
    sendRequest(request);
  }

  private HttpResponse<String> sendRequest(HttpRequest request) {
    HttpResponse<String> response;
    try {
      response = client.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }

    int statusCode = response.statusCode();
    if (statusCode < 200 || 300 <= statusCode) {
      throw new RuntimeException("Error with Vitruv-Server occurred: " + statusCode + "\n" + response.body());
    }

    return response;
  }
}
