package tools.vitruv.framework.remote.server.rest.endpoints;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import java.util.Optional;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;
import tools.vitruv.framework.remote.common.json.JsonMapper;
import tools.vitruv.framework.remote.common.rest.constants.ContentType;
import tools.vitruv.framework.remote.common.rest.constants.EndpointPath;
import tools.vitruv.framework.remote.common.rest.models.MetamodelDto;
import tools.vitruv.framework.remote.server.exception.ServerHaltingException;
import tools.vitruv.framework.remote.server.http.HttpWrapper;
import tools.vitruv.framework.remote.server.rest.GetEndpoint;
import tools.vitruv.framework.vsum.VirtualModel;
import tools.vitruv.framework.vsum.models.MetamodelReference;

/**
 * This endpoint lists the metamodels of the models of the virtual model ({@code
 * /vsum/metamodels}) or returns one metamodel as {@code .ecore} XMI ({@code
 * /vsum/metamodels/{nsUri}} with the URL-encoded namespace URI).
 */
public class MetamodelsEndpoint implements GetEndpoint {
  private final VirtualModel model;
  private final JsonMapper mapper;

  /**
   * Creates a new MetamodelsEndpoint.
   *
   * @param model The virtual model providing the metamodels.
   * @param mapper The JSON mapper to serialize the response.
   */
  public MetamodelsEndpoint(VirtualModel model, JsonMapper mapper) {
    this.model = model;
    this.mapper = mapper;
  }

  @Override
  public String process(HttpWrapper wrapper) throws ServerHaltingException {
    Optional<String> nsUri = ModelRequests.idBelow(wrapper, EndpointPath.METAMODELS);
    List<EPackage> metamodels = model.getMetamodels();
    if (nsUri.isEmpty()) {
      List<MetamodelDto> dtos =
          metamodels.stream().map(MetamodelReference::of).map(MetamodelDto::from).toList();
      wrapper.setContentType(ContentType.APPLICATION_JSON);
      try {
        return mapper.serialize(dtos);
      } catch (JsonProcessingException e) {
        throw internalServerError(e.getMessage());
      }
    }
    EPackage metamodel =
        metamodels.stream()
            .filter(p -> nsUri.get().equals(p.getNsURI()))
            .findFirst()
            .orElseThrow(() -> ModelRequests.notFound("Metamodel", nsUri.get()));
    return ModelRequests.respondXmi(wrapper, copyOf(metamodel));
  }

  /**
   * Copies the metamodel into a resource with its namespace URI, so the shared definition is not
   * attached to another resource. References to other metamodels, e.g. to the data types of Ecore,
   * keep pointing to them and are written as references to their namespace URIs.
   */
  private static Resource copyOf(EPackage metamodel) {
    EcoreUtil.Copier copier = new EcoreUtil.Copier();
    EObject copy = copier.copy(metamodel);
    copier.copyReferences();
    Resource resource = new XMIResourceImpl(URI.createURI(metamodel.getNsURI()));
    resource.getContents().add(copy);
    return resource;
  }
}
