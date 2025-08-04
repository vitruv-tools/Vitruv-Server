package tools.vitruv.framework.remote.server.http.java;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import tools.vitruv.framework.remote.server.http.VitruvHttpHandler;
import tools.vitruv.framework.remote.server.rest.PathEndointCollector;

/**
 * Represents an {@link HttpHandler}.
 */
class RequestHandler extends VitruvHttpHandler implements HttpHandler {
    RequestHandler(PathEndointCollector endpoints) {
        super(endpoints);
    }

    /**
     * Handles the request when this end point is called.
     *
     * @param exchange An object encapsulating the HTTP request and response.
     */
    @Override
    public void handle(HttpExchange exchange) {
        var method = exchange.getRequestMethod();
        var wrapper = new HttpExchangeWrapper(exchange);
        this.process(method, wrapper);
    }
}
