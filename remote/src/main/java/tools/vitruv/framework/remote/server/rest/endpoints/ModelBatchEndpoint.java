package tools.vitruv.framework.remote.server.rest.endpoints;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.ResourceSet;
import tools.vitruv.framework.remote.common.json.IdTransformation;
import tools.vitruv.framework.remote.common.json.JsonMapper;
import tools.vitruv.framework.remote.common.rest.constants.ContentType;
import tools.vitruv.framework.remote.common.rest.models.ModelBatchRequest;
import tools.vitruv.framework.remote.server.exception.ServerHaltingException;
import tools.vitruv.framework.remote.server.http.HttpWrapper;
import tools.vitruv.framework.remote.server.rest.PostEndpoint;
import tools.vitruv.framework.remote.server.rest.endpoints.ModelCopier.Format;
import tools.vitruv.framework.vsum.VirtualModel;
import tools.vitruv.framework.vsum.models.ModelReadAccess;

/**
 * This endpoint returns the content of several models at once, taken from the same state of the
 * virtual model. The optional body is a {@link ModelBatchRequest}; without ids, all models are
 * returned. The answer has the format of the models of a constraint validation: a list of {@code
 * {"uri": ..., "content": ...}}, in which every element is identified by its UUID ({@code _id}).
 *
 * <p>Responds with 404 for unknown model ids and with 400 for malformed requests.
 */
public class ModelBatchEndpoint implements PostEndpoint {
  private final VirtualModel model;
  private final JsonMapper mapper;
  private final IdTransformation transformation;

  /**
   * Creates a new ModelBatchEndpoint.
   *
   * @param model The virtual model providing the models.
   * @param mapper The JSON mapper to use.
   */
  public ModelBatchEndpoint(VirtualModel model, JsonMapper mapper) {
    this.model = model;
    this.mapper = mapper;
    this.transformation = mapper.getIdTransformation();
  }

  @Override
  public String process(HttpWrapper wrapper) throws ServerHaltingException {
    ModelBatchRequest request = readRequest(wrapper);
    Set<URI> uris = null;
    if (request.ids() != null) {
      uris = new LinkedHashSet<>();
      for (String id : request.ids()) {
        if (id == null || id.isBlank()) {
          throw new ServerHaltingException(HTTP_BAD_REQUEST, "Model ids must not be empty");
        }
        uris.add(transformation.toGlobal(URI.createURI(id)));
      }
    }
    Set<URI> requested = uris;
    ResourceSet copies = model.readModels(access -> copyModels(access, requested));
    wrapper.setContentType(ContentType.APPLICATION_JSON);
    try {
      return mapper.serialize(copies);
    } catch (JsonProcessingException e) {
      throw internalServerError(e.getMessage());
    }
  }

  private ResourceSet copyModels(ModelReadAccess access, Set<URI> uris) {
    if (uris != null) {
      for (URI uri : uris) {
        if (access.getModelResource(uri).isEmpty()) {
          throw ModelRequests.notFound("Model", transformation.toLocal(uri).toString());
        }
      }
    }
    return ModelCopier.copy(access, uris, Format.JSON);
  }

  private ModelBatchRequest readRequest(HttpWrapper wrapper) {
    try {
      String body = wrapper.getRequestBodyAsString();
      if (body == null || body.isBlank()) {
        return new ModelBatchRequest(null);
      }
      ModelBatchRequest request = mapper.deserialize(body, ModelBatchRequest.class);
      return request != null ? request : new ModelBatchRequest(null);
    } catch (JsonProcessingException e) {
      throw new ServerHaltingException(
          HTTP_BAD_REQUEST, "Invalid request: " + e.getOriginalMessage());
    } catch (IOException e) {
      throw new ServerHaltingException(HTTP_BAD_REQUEST, "Could not read request body");
    }
  }
}
