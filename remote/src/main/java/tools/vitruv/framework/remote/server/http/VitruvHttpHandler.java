package tools.vitruv.framework.remote.server.http;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import tools.vitruv.framework.remote.common.rest.constants.ContentType;
import tools.vitruv.framework.remote.server.exception.ServerHaltingException;
import tools.vitruv.framework.remote.server.rest.PathEndointCollector;

public class VitruvHttpHandler {
    private static final int HTTP_NOT_FOUND = 404;
    private static final int HTTP_OK = 200;
    private static final int HTTP_INTERNAL_ERROR = 500;

    private PathEndointCollector endpoints;

    public VitruvHttpHandler(PathEndointCollector endpoints) {
    	this.endpoints = endpoints;
    }

    /**
     * Processes an request when this end point is called.
     *
     * @param wrapper An object encapsulating the HTTP request and response.
     */
    public void process(String method, HttpWrapper wrapper) {
        try {
            var response = switch (method) {
                case "GET" -> endpoints.getEndpoint().process(wrapper);
                case "PUT" -> endpoints.putEndpoint().process(wrapper);
                case "POST" -> endpoints.postEndpoint().process(wrapper);
                case "PATCH" -> endpoints.patchEndpoint().process(wrapper);
                case "DELETE" -> endpoints.deleteEndpoint().process(wrapper);
                default -> throw new ServerHaltingException(HTTP_NOT_FOUND, "Request method not supported!");
            };
            if (response != null) {
                wrapper.sendResponse(HTTP_OK, response.getBytes(StandardCharsets.UTF_8));
            } else {
                wrapper.sendResponse(HTTP_OK);
            }
        } catch (Exception exception) {
            var statusCode = HTTP_INTERNAL_ERROR;
            if (exception instanceof ServerHaltingException haltingException) {
                statusCode = haltingException.getStatusCode();
            }
            wrapper.setContentType(ContentType.TEXT_PLAIN);
            try {
            	wrapper.sendResponse(statusCode, exception.getMessage().getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
            	throw new IllegalStateException("Sending a response (" + statusCode + " " + exception.getMessage() + ") failed.", e);
            }
        }
    }
}
