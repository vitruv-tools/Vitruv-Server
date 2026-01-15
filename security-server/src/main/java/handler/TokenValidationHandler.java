package handler;

import app.VitruvSecurityServerApp;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.TokenUtils;

import java.io.IOException;

/**
 *  Acts as a security wrapper for the VitruvRequestHandler, ensuring only valid Access Tokens are processed.
 *  If a token is expired or missing, it attempts renewal using a Refresh Token.
 *  If unsuccessful, clients are redirected to the '/auth' endpoint.
 */
public class TokenValidationHandler implements HttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(TokenValidationHandler.class);
    private final HttpHandler next;
    private final AuthEndpointHandler authEndpointHandler = new AuthEndpointHandler();

    /**
     * @param next The handler to call if the request is authorized.
     */
    public TokenValidationHandler(HttpHandler next) {
        this.next = next;
    }

    /**
     * Checks access token validity, handles refresh if needed, or redirects to authentication.
     *
     * @param exchange HTTP exchange containing request data
     * @throws IOException if forwarding or redirecting fails
     */
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

    /**
     * Attempts to refresh the access token using the refresh token. If unsuccessful, redirects to authentication.
     *
     * @param exchange HTTP exchange containing request data
     * @throws IOException if token refresh fails
     */
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

    /**
     * Clears old tokens and sets new access and refresh tokens as cookies.
     *
     * @param exchange HTTP exchange containing request data
     * @param refreshToken refresh Token
     * @throws Exception if token refresh fails
     */
    private void replaceTokens(HttpExchange exchange, String refreshToken) throws Exception {
        AccessTokenResponse newTokens = VitruvSecurityServerApp.getOidcClient().refreshAccessToken(refreshToken);
        String newAccessToken = newTokens.getTokens().getAccessToken().getValue();
        String newRefreshToken = newTokens.getTokens().getRefreshToken().getValue();

        logger.debug("New Access Token: {}", newAccessToken);
        logger.debug("New Refresh Token: {}", newRefreshToken);

        // remove old tokens
        exchange.getResponseHeaders().add("Set-Cookie", "access_token=; Max-Age=0; Path=/; HttpOnly; Secure; SameSite=Strict");
        exchange.getResponseHeaders().add("Set-Cookie", "refresh_token=; Max-Age=0; Path=/; HttpOnly; Secure; SameSite=Strict");

        // set new tokens
        exchange.getResponseHeaders().add("Set-Cookie", "access_token=" + newAccessToken + "; Path=/; HttpOnly; Secure; SameSite=Strict");
        exchange.getResponseHeaders().add("Set-Cookie", "refresh_token=" + newRefreshToken + "; Path=/; HttpOnly; Secure; SameSite=Strict");
    }
}
