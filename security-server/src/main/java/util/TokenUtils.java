package util;

import com.sun.net.httpserver.HttpExchange;

import java.util.List;

public class TokenUtils {
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