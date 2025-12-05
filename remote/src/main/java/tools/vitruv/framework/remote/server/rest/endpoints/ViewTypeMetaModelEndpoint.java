package tools.vitruv.framework.remote.server.rest.endpoints;

import java.util.Optional;

import tools.vitruv.framework.remote.common.rest.constants.EndpointPath;
import tools.vitruv.framework.remote.server.exception.ServerHaltingException;
import tools.vitruv.framework.remote.server.http.HttpWrapper;
import tools.vitruv.framework.remote.server.rest.GetEndpoint;
import tools.vitruv.framework.views.ViewType;
import tools.vitruv.framework.vsum.VirtualModel;

public class ViewTypeMetaModelEndpoint implements GetEndpoint {
    private final VirtualModel model;

    public ViewTypeMetaModelEndpoint(VirtualModel model) {
        this.model = model;
    }

    @Override
    public String process(HttpWrapper wrapper) throws ServerHaltingException {
        String requestPath = wrapper.getRequestURI().getPath();
        if (requestPath == null || !requestPath.startsWith(EndpointPath.VIEW_TYPES_METAMODEL) || requestPath.equals(EndpointPath.VIEW_TYPES_METAMODEL)) {
            throw internalServerError("Request cannot be processed.");
        }

        String viewTypeName = requestPath.substring(EndpointPath.VIEW_TYPES_METAMODEL.length());
        Optional<ViewType<?>> viewType = model.getViewTypes().stream().filter(vt -> vt.getName().equals(viewTypeName)).findFirst();

        if (viewType.isEmpty()) {
            throw notFound("View type with name " + viewTypeName + " not found.");
        }
        if (viewType.get().getMetamodel() == null) {
            throw notFound("View type " + viewTypeName + " has no metamodel to send.");
        }

        return ""; // Here, map view type metamodel to a String.
    }
}
