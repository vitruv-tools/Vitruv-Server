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

//        String response = "Hey :) You wanna call me back?";
//        exchange.sendResponseHeaders(200, response.getBytes().length);
//        exchange.getResponseBody().write(response.getBytes());
//        exchange.close();


        String query = exchange.getRequestURI().getQuery();
        String code = getQueryParam(query, "code");

        if (code == null) {
            String response = "Authorization code not found in the callback request.";
            exchange.sendResponseHeaders(400, response.getBytes().length);
            exchange.getResponseBody().write(response.getBytes());
            exchange.close();
            return;
        }

        try {
            Tokens tokens = VitruvServerApp.getOidcClient().exchangeAuthorizationCode(code);
            logger.info("Access Token received: {}", tokens.getAccessToken().getValue());

            String response = "Authorization successful! Access Token: " + tokens.getAccessToken().getValue();
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