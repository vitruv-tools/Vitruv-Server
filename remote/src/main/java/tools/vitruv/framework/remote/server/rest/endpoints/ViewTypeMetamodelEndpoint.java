package tools.vitruv.framework.remote.server.rest.endpoints;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.eclipse.emf.ecore.EPackage;
import tools.vitruv.framework.remote.common.json.JsonMapper;
import tools.vitruv.framework.remote.common.rest.constants.ContentType;
import tools.vitruv.framework.remote.common.rest.constants.Header;
import tools.vitruv.framework.remote.server.exception.ServerHaltingException;
import tools.vitruv.framework.remote.server.http.HttpWrapper;
import tools.vitruv.framework.remote.server.rest.GetEndpoint;
import tools.vitruv.framework.views.ViewType;
import tools.vitruv.framework.vsum.VirtualModel;

/**
 * A {@link ViewTypeMetamodelEndpoint} provides metamodels {@link EPackage}s
 * that underlie a {@link ViewType}.
 */
public class ViewTypeMetamodelEndpoint implements GetEndpoint {
  private final VirtualModel vsum;
  private final JsonMapper mapper;

  /**
   * Creates a new ViewTypeMetamodelEndpoint.
   *
   * @param vsum - {@link VirtualModel}
   * @param mapper - {@link JsonMapper}
   */
  public ViewTypeMetamodelEndpoint(VirtualModel vsum, JsonMapper mapper) {
    this.vsum = vsum;
    this.mapper = mapper;
  }

  /**
   * Looks up and serializes the metamodel {@link EPackage} as JSON for the view type 
   * whose name is given by the <code>View-Type</code> header in <code>wrapper</code>.
   *
   * @throws ServerHaltingException with response code 404 if <code>View-Type</code> does not map
   *      to a view type defined in <code>virtualModel</code>,
   *      or 500, if the metamodel cannot be serialized.
   */
  @Override
  public String process(HttpWrapper wrapper) throws ServerHaltingException {
    // Look up the view type; return 404, if it does not exist.
    var viewTypeName = wrapper.getRequestHeader(Header.VIEW_TYPE);
    var viewType = vsum.getViewTypes()
        .stream()
        .filter(vt -> vt.getName().equals(viewTypeName))
        .findFirst();
    if (viewType.isEmpty()) {
      throw notFound("View Type with name " + viewTypeName + " not found!");
    }

    // Serialize the EPackage.
    var metamodel = viewType.get().getMetamodel();
    wrapper.setContentType(ContentType.APPLICATION_JSON);
    try {
      return mapper.serialize(metamodel);
    } catch (JsonProcessingException e) {
      throw internalServerError("Failed to serialize EPackage: " + e.getMessage());
    }
  }
  
}
