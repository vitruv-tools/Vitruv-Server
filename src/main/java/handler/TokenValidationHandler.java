package handler;

import app.VitruvServerApp;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.Date;

public class TokenValidationHandler implements HttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(TokenValidationHandler.class);
    private final HttpHandler next;
    private final AuthEndpointHandler authEndpointHandler = new AuthEndpointHandler();

    public TokenValidationHandler(HttpHandler next) {
        this.next = next;
    }

    @Override
    public void handle(HttpExchange exchange) {
        try {
            String accessToken;
            String idToken;

            try {
                accessToken = extractToken(exchange, "access_token");
                idToken = extractToken(exchange, "id_token");
            } catch (Exception e) {
                logger.error("Token extraction from cookies failed: {} -> Redirecting to SSO.", e.getMessage());
                authEndpointHandler.handle(exchange);
                return;
            }

            try {
                VitruvServerApp.getOidcClient().validateIDToken(idToken);
            } catch (Exception e) {
                logger.error("ID Token validation failed: {} -> Redirecting to SSO.", e.getMessage());
                authEndpointHandler.handle(exchange);
                return;
            }

            if (accessToken == null || !isValidAccessToken(accessToken)) {
                logger.warn("Invalid or missing Access Token. Redirecting to SSO.");
                authEndpointHandler.handle(exchange);
                return;
            }
            logger.info("Tokens of client are valid.");
            next.handle(exchange);
        }
        catch (Exception e) {
            logger.error("An error occurred while validating Access and ID Tokens: {}", e.getMessage(), e);
        }
    }

    private String extractToken(HttpExchange exchange, String tokenType) {
        String[] tokens = exchange.getRequestHeaders().get("Cookie").get(0).split(";");

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
}
