package tools.vitruv.remote.secserver.util;

import com.sun.net.httpserver.HttpExchange;

import java.util.List;

/**
 * Utility for tokens.
 */
public class TokenUtils {

    /**
     * Extracts the value of a specific token from the Cookie header.
     *
     * @param exchange HTTP exchange containing the request
     * @param tokenType name of the token to extract (e.g. "access_token")
     * @return token value, or null if not found
     */
    public static String extractToken(HttpExchange exchange, String tokenType) {
        List<String> cookieHeaders = exchange.getRequestHeaders().get("Cookie");

        if (cookieHeaders == null || cookieHeaders.isEmpty()) {
            return null;
        }

        String[] tokens = cookieHeaders.get(0).split(";");

        for (String token : tokens) {
            token = token.trim();
            if (token.startsWith(tokenType + "=")) {
                return token.substring((tokenType + "=").length());
            }
        }
        return null;
    }
}