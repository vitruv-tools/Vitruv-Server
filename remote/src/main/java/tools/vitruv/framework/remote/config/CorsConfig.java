package tools.vitruv.framework.remote.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * Configuration class for setting up Cross-Origin Resource Sharing (CORS) in the application.
 * This class defines the CORS policy for handling cross-origin requests, specifying allowed
 * origins, headers, and HTTP methods.
 * <p>
 * The configuration allows credentials to be included in cross-origin requests and registers
 * the CORS policies for all endpoints using a specified URL pattern.
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowCredentials(true);
        // Dev: localhost + LAN peers (two devices on same Wi‑Fi testing the UI).
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:5173",
                "http://127.0.0.1:5173",
                "http://192.168.*.*:5173",
                "http://10.*.*.*:5173",
                "http://172.*.*.*:5173"
        ));
        config.setAllowedHeaders(List.of(
                "Origin",
                "Content-Type",
                "Accept",
                "Authorization",
                "Access-Control-Allow-Origin"
        ));
        config.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "DELETE",
                "PATCH",
                "OPTIONS"
        ));

        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
