package org.j2ra.minierp.gateway.config;

import org.j2ra.minierp.gateway.GatewayProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * CORS is handled here and nowhere else (ADR-010): the services set no CORS headers.
 * SecurityConfig uses this one source, so a preflight (it has no token) is answered before authentication.
 */
@Configuration
class CorsConfig {

    @Bean
    CorsConfigurationSource corsConfigurationSource(GatewayProperties properties) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(List.of(properties.cors().allowedOrigin()));
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Correlation-Id"));
        // Without this the browser cannot read the id, even though the response has it.
        cors.setExposedHeaders(List.of("X-Correlation-Id"));
        // The token is in the Authorization header, not in a cookie.
        cors.setAllowCredentials(false);
        cors.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }
}
