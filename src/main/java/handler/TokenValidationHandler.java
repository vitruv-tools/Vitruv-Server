package handler;

import app.VitruvServerApp;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

public class TokenValidationHandler implements HttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(TokenValidationHandler.class);
    private final HttpHandler next;
    private final AuthEndpointHandler authEndpointHandler = new AuthEndpointHandler();

    public TokenValidationHandler(HttpHandler next) {
        this.next = next;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        logger.info("\nNew Request: '{}'", exchange.getRequestURI());
        try {
            String accessToken = extractToken(exchange, "access_token");

            // check if Access Token is valid
            if (accessToken != null && isValidAccessToken(accessToken)) {
                logger.info("Access Token is valid. Processing request.");
                next.handle(exchange);
            } else { // Access Token is not valid
                handleTokenRefresh(exchange);
            }
        } catch (Exception e) {
            logger.error("An error occurred while validating Access Token: {} -> Redirecting to SSO.", e.getMessage());
            authEndpointHandler.handle(exchange);
        }
    }

    private String extractToken(HttpExchange exchange, String tokenType) {
        List<String> cookieHeaders = exchange.getRequestHeaders().get("Cookie");

        if (cookieHeaders == null || cookieHeaders.isEmpty()) {
            logger.warn("No cookies found in request.");
            return null;
        }

        String[] tokens = cookieHeaders.get(0).split(";");

        for (String token : tokens) {
            token = token.trim();
            if (token.startsWith(tokenType + "=")) {
                return token.substring((tokenType + "=").length());
            }
        }
        logger.warn("{} is empty", tokenType);
        return null;
    }

    private boolean isValidAccessToken(String accessToken) {
        try {
            JWTClaimsSet jwtClaimsSet = SignedJWT.parse(accessToken).getJWTClaimsSet();

            // check if Access Token is expired
            String scopedAffiliation = jwtClaimsSet.getClaim("eduperson_scoped_affiliation").toString();
            if (!scopedAffiliation.contains("member@kit.edu")) {
                logger.error("Client is no member of KIT");
                return false;
            }

            // check if Access Token is expired
            Date expiration = jwtClaimsSet.getExpirationTime();
            if (expiration == null || expiration.before(new Date())) {
                logger.error("Access Token expired");
                return false;
            }
            logger.info("Access Token is valid");
            return true;
        } catch (ParseException e) {
            logger.error("Error parsing Access Token: {}", e.getMessage());
            return false;
        }
    }

    private void handleTokenRefresh(HttpExchange exchange) throws IOException {
        String refreshToken = extractToken(exchange, "refresh_token");

        if (refreshToken == null) {
            logger.warn("No valid Access Token and no Refresh Token found. Redirecting to SSO.");
            authEndpointHandler.handle(exchange);
            return;
        }

        // try to refresh Access Token and Refresh Token
        try {
            AccessTokenResponse newTokens = VitruvServerApp.getOidcClient().refreshAccessToken(refreshToken);
            String newAccessToken = newTokens.getTokens().getAccessToken().getValue();
            String newRefreshToken = newTokens.getTokens().getRefreshToken().getValue();

            // remove old tokens
            exchange.getResponseHeaders().add("Set-Cookie", "access_token=; Max-Age=0; Path=/; HttpOnly; Secure; SameSite=Strict");
            exchange.getResponseHeaders().add("Set-Cookie", "refresh_token=; Max-Age=0; Path=/; HttpOnly; Secure; SameSite=Strict");

            // set new tokens (TODO: set 'Secure;' flag -> Cookie can only be sent with HTTPS)
            exchange.getResponseHeaders().add("Set-Cookie", "access_token=" + newAccessToken + "; Path=/; HttpOnly; SameSite=Strict");
            if (newRefreshToken != null) {
                exchange.getResponseHeaders().add("Set-Cookie", "refresh_token=" + newRefreshToken + "; Path=/; HttpOnly; SameSite=Strict");
            } else {
                logger.warn("No new Refresh Token returned. Keeping the old one.");
            }

            logger.info("Access Token successfully refreshed. Processing request.");
            next.handle(exchange);

        } catch (Exception e) {
            logger.error("Failed to refresh Access Token: {} -> Redirecting to SSO.", e.getMessage());
            authEndpointHandler.handle(exchange);
        }
    }
}
