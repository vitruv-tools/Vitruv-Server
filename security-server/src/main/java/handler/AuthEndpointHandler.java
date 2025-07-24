package handler;

import oidc.OIDCClient;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Handles the `/auth` endpoint. Redirects clients to the OIDC authorization page provided by FeLS to initiate
 * SSO authentication, since only authenticated users can use endpoints of the Vitruv Server.
 */
public class AuthEndpointHandler implements HttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(AuthEndpointHandler.class);
    private OIDCClient oidcClient;

    public AuthEndpointHandler(OIDCClient client) {
        oidcClient = client;
    }

    /**
     * Redirects incoming requests to the OIDC authentication URL of FeLS.
     *
     * @param exchange HTTP exchange containing the request and response data
     * @throws IOException if io error occurs during redirect
     */
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String authorizationUrl = oidcClient.getAuthorizationRequestURI().toString();
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
