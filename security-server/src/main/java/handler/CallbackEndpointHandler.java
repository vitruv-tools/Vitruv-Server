package handler;

import app.VitruvServerApp;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.token.Tokens;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class CallbackEndpointHandler implements HttpHandler {

    private static final Logger logger = LoggerFactory.getLogger(CallbackEndpointHandler.class);

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        logger.info("Handling callback endpoint: {}", exchange.getRequestURI());

        String query = exchange.getRequestURI().getQuery();
        String code = getCode(query);

        if (code == null) {
            handleMissingAuthCode(exchange);
            return;
        }
        logger.debug("Access code received: {}", code);

        try {
            // get tokens
            AccessTokenResponse tokenResponse = VitruvServerApp.getOidcClient().exchangeAuthorizationCode(code);
            Tokens tokens = tokenResponse.getTokens();

            String idToken = tokenResponse.getCustomParameters().get("id_token").toString();
            String accessToken = tokens.getAccessToken().getValue();
            String refreshToken = tokens.getRefreshToken().getValue();
            logger.debug("ID Token: {}", idToken);
            logger.debug("Access Token: {}", accessToken);
            logger.debug("Refresh Token: {}", refreshToken);

            // validate ID Token
            VitruvServerApp.getOidcClient().validateIDToken(idToken);

            handleSuccessResponse(exchange, idToken, accessToken, refreshToken);

        } catch (Exception e) {
            logger.error("Error during token exchange: {}", e.getMessage());
            handleUnsuccessfulResponse(exchange);
        } finally {
            exchange.close();
        }
    }

    private void handleMissingAuthCode(HttpExchange exchange) throws IOException {
        String response = "Authorization code not found in the callback request.";
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
        String response = "Authorization successful! You can send new requests now." + "\n\n"
                + "Access Token (JWT, expires in 1 hour):\n" + accessToken + "\n\n"
                + "ID Token (JWT; expires in 1 hour):\n" + idToken + "\n\n"
                + "Refresh Token (Opaque; no expiration information):\n" + refreshToken;
        exchange.sendResponseHeaders(200, response.getBytes().length);
        exchange.getResponseBody().write(response.getBytes());
    }

    private void handleUnsuccessfulResponse(HttpExchange exchange) throws IOException {
        String response = "Token exchange failed.";
        exchange.sendResponseHeaders(500, response.getBytes().length);
        exchange.getResponseBody().write(response.getBytes());
    }

    private String getCode(String query) {
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