package tools.vitruv.framework.remote.server.rest.endpoints;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import tools.vitruv.framework.remote.common.json.IdTransformation;
import tools.vitruv.framework.remote.common.json.JsonFieldName;
import tools.vitruv.framework.remote.common.json.JsonMapper;
import tools.vitruv.framework.remote.common.rest.constants.ContentType;
import tools.vitruv.framework.remote.common.rest.constants.EndpointPath;
import tools.vitruv.framework.remote.common.rest.models.MetamodelDto;
import tools.vitruv.framework.remote.common.rest.models.ModelDto;
import tools.vitruv.framework.remote.server.exception.ServerHaltingException;
import tools.vitruv.framework.remote.server.http.HttpWrapper;
import tools.vitruv.framework.remote.server.rest.GetEndpoint;
import tools.vitruv.framework.remote.server.rest.endpoints.ModelCopier.Format;
import tools.vitruv.framework.vsum.VirtualModel;
import tools.vitruv.framework.vsum.models.ModelDescriptor;
import tools.vitruv.framework.vsum.models.ModelReadAccess;

/**
 * This endpoint lists the models of the virtual model ({@code /vsum/models}) or returns the content
 * of one model ({@code /vsum/models/{id}}), as JSON or, with {@code Accept: application/xmi+xml},
 * as XMI. In both formats every element is identified by its UUID in the virtual model.
 */
public class ModelsEndpoint implements GetEndpoint {
  private final VirtualModel model;
  private final JsonMapper mapper;
  private final IdTransformation transformation;

  /**
   * Creates a new ModelsEndpoint.
   *
   * @param model The virtual model providing the models.
   * @param mapper The JSON mapper to serialize the response.
   */
  public ModelsEndpoint(VirtualModel model, JsonMapper mapper) {
    this.model = model;
    this.mapper = mapper;
    this.transformation = mapper.getIdTransformation();
  }

  @Override
  public String process(HttpWrapper wrapper) throws ServerHaltingException {
    Optional<String> id = ModelRequests.idBelow(wrapper, EndpointPath.MODELS);
    if (id.isEmpty()) {
      List<ModelDto> models = model.getModels().stream().map(this::toDto).toList();
      return respondJson(wrapper, models);
    }
    URI uri = transformation.toGlobal(URI.createURI(id.get()));
    Format format = ModelRequests.acceptsXmi(wrapper) ? Format.XMI : Format.JSON;
    ResourceSet copies = model.readModels(access -> copyModel(access, uri, format));
    if (copies == null) {
      throw ModelRequests.notFound("Model", id.get());
    }
    Resource copy = copies.getResources().get(0);
    if (format == Format.XMI) {
      return ModelRequests.respondXmi(wrapper, copy);
    }
    Map<String, Object> body = new LinkedHashMap<>();
    body.put(JsonFieldName.URI, transformation.toLocal(copy.getURI()).toString());
    body.put(JsonFieldName.CONTENT, copy);
    return respondJson(wrapper, body);
  }

  private static ResourceSet copyModel(ModelReadAccess access, URI uri, Format format) {
    return access.getModelResource(uri).isPresent()
        ? ModelCopier.copy(access, Set.of(uri), format)
        : null;
  }

  private ModelDto toDto(ModelDescriptor descriptor) {
    return new ModelDto(
        transformation.toLocal(descriptor.uri()).toString(),
        descriptor.metamodels().stream().map(MetamodelDto::from).toList(),
        descriptor.rootCount());
  }

  private String respondJson(HttpWrapper wrapper, Object body) {
    wrapper.setContentType(ContentType.APPLICATION_JSON);
    try {
      return mapper.serialize(body);
    } catch (JsonProcessingException e) {
      throw internalServerError(e.getMessage());
    }
  }
}
