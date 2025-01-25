package server;

import app.VitruvServerApp;
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
        logger.info("Access code received: {}", code);

        try {
            Tokens tokens = VitruvServerApp.getOidcClient().exchangeAuthorizationCode(code);
            logger.info("Access token received: {}", tokens.getAccessToken().getValue());
            logger.info("Access refresh token received: {}", tokens.getRefreshToken().getValue());

            String accessToken = tokens.getAccessToken().getValue();

            // TODO: set 'Secure' flag -> Cookie can only be sent with HTTPS
//            exchange.getResponseHeaders().add("Set-Cookie", "id_token=" + "TODO" + "; HttpOnly; Secure; SameSite=Strict");
//            exchange.getResponseHeaders().add("Set-Cookie", "access_token=" + accessToken+ "; HttpOnly; Secure; SameSite=Strict");

            exchange.getResponseHeaders().add("Set-Cookie", "id_token=" + "TODO" + "; HttpOnly; SameSite=Strict");
            exchange.getResponseHeaders().add("Set-Cookie", "access_token=" + accessToken + "; HttpOnly; SameSite=Strict");

            String response = "Authorization successful! Access token: " + accessToken;
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