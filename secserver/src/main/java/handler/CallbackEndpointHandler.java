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
        String code = getQueryParam(query, "code");

        if (code == null) {
            String response = "Authorization code not found in the callback request.";
            logger.info(response);
            exchange.sendResponseHeaders(400, response.getBytes().length);
            exchange.getResponseBody().write(response.getBytes());
            exchange.close();
            return;
        }
        logger.debug("Access code received: {}", code);

        try {
            // get ID Token and Access Token
            AccessTokenResponse tokenResponse = VitruvServerApp.getOidcClient().exchangeAuthorizationCode(code);

            String idToken = tokenResponse.getCustomParameters().get("id_token").toString();
            logger.debug("ID Token: {}", idToken);

            Tokens tokens = tokenResponse.getTokens();
            String accessToken = tokens.getAccessToken().getValue();
            String refreshToken = tokens.getRefreshToken().getValue();

            logger.debug("Access Token: {}", accessToken);
            logger.debug("Refresh Token: {}", refreshToken);

            // validate ID Token
            VitruvServerApp.getOidcClient().validateIDToken(idToken);

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
        } catch (Exception e) {
            logger.error("Error during token exchange: {}", e.getMessage());
            String response = "Token exchange failed.";
            exchange.sendResponseHeaders(500, response.getBytes().length);
            exchange.getResponseBody().write(response.getBytes());
        } finally {
            exchange.close();
        }
    }

    private String getQueryParam(String query, String param) {
        if (query == null || param == null) return null;
        for (String pair : query.split("&")) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2 && keyValue[0].equals(param)) {
                return keyValue[1];
            }
        }
        return null;
    }
}