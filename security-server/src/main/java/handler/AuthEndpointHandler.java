package handler;

import app.VitruvSecurityServerApp;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class AuthEndpointHandler implements HttpHandler {

    private static final Logger logger = LoggerFactory.getLogger(AuthEndpointHandler.class);

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String authorizationUrl = VitruvSecurityServerApp.getOidcClient().getAuthorizationRequestURI().toString();
            logger.info("SSO redirect to authentication URL: {}", authorizationUrl);
            exchange.getResponseHeaders().set("Location", authorizationUrl);
            // code 302 for redirect, -1 for empty body
            exchange.sendResponseHeaders(302, -1);
        } catch (Exception e) {
            logger.error("Error generating authentication URL: {}", e.getMessage());
            exchange.sendResponseHeaders(500, 0);
        } finally {
            exchange.close();
        }
    }
}
