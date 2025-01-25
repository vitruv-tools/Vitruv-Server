package server;

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
        logger.info("Access code received: {}", code);

        try {
            // get ID Token and Access Token
            AccessTokenResponse tokenResponse = VitruvServerApp.getOidcClient().exchangeAuthorizationCode(code);

            String idToken = tokenResponse.getCustomParameters().get("id_token").toString();
            logger.info("ID Token: {}", idToken);

            Tokens tokens = tokenResponse.getTokens();
            String accessToken = tokens.getAccessToken().getValue();
            String refreshToken = tokens.getRefreshToken().getValue();

            logger.info("Access Token: {}", accessToken);
            logger.info("Refresh Token: {}", refreshToken);

            // validate ID Token
            VitruvServerApp.getOidcClient().validateIDToken(idToken);

            // set cookie (TODO: set 'Secure;' flag -> Cookie can only be sent with HTTPS)
            exchange.getResponseHeaders().add("Set-Cookie", "id_token=" + idToken + "; HttpOnly; SameSite=Strict");
            exchange.getResponseHeaders().add("Set-Cookie", "access_token=" + accessToken + "; HttpOnly; SameSite=Strict");
            exchange.getResponseHeaders().add("Set-Cookie", "refresh_token=" + refreshToken + "; HttpOnly; SameSite=Strict; Path=/auth");

            // set body
            String response = "Authorization successful!" + "\n\n"
                    + "Access Token: " + accessToken + "\n\n"
                    + "ID Token: " + idToken + "\n\n"
                    + "Refresh Token: " + refreshToken;
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