package tools.vitruv.remote.secserver.handler;

import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.token.Tokens;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import tools.vitruv.remote.secserver.oidc.OIDCClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Handles the `/callback` endpoint. It processes incoming authorization codes from an OIDC provider and exchanges them for ID, Access,
 * and Refresh Tokens. After validation of ID Token, all tokens are sent to the client. The tokens are stored as
 * HTTP-only cookies for secure client-side storage, and secure flag ensures HTTPS usage of the cookies.
 */
public class CallbackEndpointHandler implements HttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(CallbackEndpointHandler.class);
    private OIDCClient oidcClient;

    public CallbackEndpointHandler(OIDCClient client) {
        oidcClient = client;
    }

    /**
     * Processes incoming requests, exchanges the authorization code for tokens, and sets authentication cookies.
     *
     * @param exchange HTTP exchange containing the request and response data
     * @throws IOException if io error occurs during handling
     */
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        logger.info("Handling callback endpoint: {}", exchange.getRequestURI());

        String query = exchange.getRequestURI().getQuery();
        String authorizationCode = getAuthorizationCode(query);

        if (authorizationCode == null) {
            handleMissingAuthCode(exchange);
            return;
        }
        logger.debug("OIDC Authorization code received: {}", authorizationCode);

        try {
            // get tokens
            AccessTokenResponse tokenResponse = oidcClient.exchangeAuthorizationCode(authorizationCode);
            Tokens tokens = tokenResponse.getTokens();

            String idToken = tokenResponse.getCustomParameters().get("id_token").toString();
            String accessToken = tokens.getAccessToken().getValue();
            String refreshToken = tokens.getRefreshToken().getValue();
            logger.debug("ID Token: {}", idToken);
            logger.debug("Access Token: {}", accessToken);
            logger.debug("Refresh Token: {}", refreshToken);

            // validate ID Token
            oidcClient.validateIDToken(idToken);

            handleSuccessResponse(exchange, idToken, accessToken, refreshToken);

        } catch (Exception e) {
            logger.error("Error during token exchange: {}", e.getMessage());
            handleUnsuccessfulResponse(exchange);
        } finally {
            exchange.close();
        }
    }

    private void handleMissingAuthCode(HttpExchange exchange) throws IOException {
        String response = "Authentication failed. OIDC Authorization code not found in the callback request.";
        logger.info(response);
        exchange.sendResponseHeaders(400, response.getBytes().length);
        exchange.getResponseBody().write(response.getBytes());
        exchange.close();
    }

    private void handleSuccessResponse(HttpExchange exchange, String idToken, String accessToken, String refreshToken) throws IOException {
        // set cookies
        exchange.getResponseHeaders().add("Set-Cookie", "id_token=" + idToken + "; Path=/; HttpOnly; Secure; SameSite=Strict");
        exchange.getResponseHeaders().add("Set-Cookie", "access_token=" + accessToken + "; Path=/; HttpOnly; Secure; SameSite=Strict");
        exchange.getResponseHeaders().add("Set-Cookie", "refresh_token=" + refreshToken + "; Path=/; HttpOnly; Secure; SameSite=Strict");

        // set body
        String response = "You were successfully authenticated! Your requests are authorized now." + "\n\n"
                + "Access Token (JWT; expires in 1 hour):\n" + accessToken + "\n\n"
                + "ID Token (JWT; expires in 1 hour):\n" + idToken + "\n\n"
                + "Refresh Token (Opaque; this token is not further required):\n" + refreshToken;
        exchange.sendResponseHeaders(200, response.getBytes().length);
        exchange.getResponseBody().write(response.getBytes());
    }

    private void handleUnsuccessfulResponse(HttpExchange exchange) throws IOException {
        String response = "Token exchange failed.";
        exchange.sendResponseHeaders(500, response.getBytes().length);
        exchange.getResponseBody().write(response.getBytes());
    }

    private String getAuthorizationCode(String query) {
        if (query == null) return null;
        for (String pair : query.split("&")) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2 && keyValue[0].equals("code")) {
                return keyValue[1];
            }
        }
        return null;
    }
}