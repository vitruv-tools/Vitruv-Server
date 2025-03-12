package handler;

import app.VitruvSecurityServerApp;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.TokenUtils;

import java.io.IOException;

public class TokenValidationHandler implements HttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(TokenValidationHandler.class);
    private final HttpHandler next;
    private final AuthEndpointHandler authEndpointHandler = new AuthEndpointHandler();

    public TokenValidationHandler(HttpHandler next) {
        this.next = next;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (exchange.getRequestURI().toString().equals("/favicon.ico")) {
            logger.info("Ignore '/favicon.ico' request.");
            return;
        }

        logger.info("\nNew Request: '{}'", exchange.getRequestURI().toString());
        try {
            String accessToken = TokenUtils.extractToken(exchange, "access_token");

            // check if Access Token is valid
            if (accessToken != null && VitruvSecurityServerApp.getOidcClient().isAccessTokenValid(accessToken)) {
                next.handle(exchange);
            }
            else {
                // try to refresh Access Token
                handleTokenRefresh(exchange);
            }
        } catch (Exception e) {
            logger.error("An error occurred while validating Access Token: {}\n-> Redirecting to SSO.", e.getMessage());
            authEndpointHandler.handle(exchange);
        }
    }

    private void handleTokenRefresh(HttpExchange exchange) throws IOException {
        String refreshToken = TokenUtils.extractToken(exchange, "refresh_token");

        if (refreshToken == null) {
            logger.warn("No valid Access Token and no Refresh Token found.\n-> Redirecting to SSO.");
            authEndpointHandler.handle(exchange);
            return;
        }

        // try to refresh Access Token and Refresh Token
        try {
            replaceTokens(exchange, refreshToken);

            logger.info("Access Token successfully refreshed. Processing request.");
            next.handle(exchange);

        } catch (Exception e) {
            logger.error("Failed to refresh Access Token: {}\n-> Redirecting to SSO.", e.getMessage());
            authEndpointHandler.handle(exchange);
        }
    }

    private void replaceTokens(HttpExchange exchange, String refreshToken) throws Exception {
        AccessTokenResponse newTokens = VitruvSecurityServerApp.getOidcClient().refreshAccessToken(refreshToken);
        String newAccessToken = newTokens.getTokens().getAccessToken().getValue();
        String newRefreshToken = newTokens.getTokens().getRefreshToken().getValue();

        // remove old tokens
        exchange.getResponseHeaders().add("Set-Cookie", "access_token=; Max-Age=0; Path=/; HttpOnly; Secure; SameSite=Strict");
        exchange.getResponseHeaders().add("Set-Cookie", "refresh_token=; Max-Age=0; Path=/; HttpOnly; Secure; SameSite=Strict");

        // set new tokens
        exchange.getResponseHeaders().add("Set-Cookie", "access_token=" + newAccessToken + "; Path=/; HttpOnly; Secure; SameSite=Strict");
        exchange.getResponseHeaders().add("Set-Cookie", "refresh_token=" + newRefreshToken + "; Path=/; HttpOnly; Secure; SameSite=Strict");
    }
}
