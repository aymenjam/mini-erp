package org.j2ra.minierp.gateway;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

/**
 * Settings of the gateway. Both come from environment variables (.env.example) and have no default outside the profile dev.
 *
 * <p>The checks are needed because the binder of Spring Boot does not fail on a placeholder it cannot resolve: it keeps the text
 * "${CORS_ALLOWED_ORIGIN}" as the value. Without a check the gateway would start with a nonsense origin.
 */
@ConfigurationProperties("gateway")
public record GatewayProperties(URI customersUri, Cors cors) {

    public GatewayProperties {
        if (customersUri == null || customersUri.getScheme() == null || customersUri.getHost() == null) {
            throw new IllegalArgumentException("CUSTOMERS_URI is not set or is not a full URL (example: http://localhost:8091)");
        }
        if (cors == null) {
            throw new IllegalArgumentException("CORS_ALLOWED_ORIGIN is not set");
        }
    }

    public record Cors(String allowedOrigin) {

        public Cors {
            if (allowedOrigin == null || allowedOrigin.isBlank() || allowedOrigin.contains("${")) {
                throw new IllegalArgumentException("CORS_ALLOWED_ORIGIN is not set");
            }
            // One exact origin, like http://localhost:4200. "*" would open the API to every web page.
            if (allowedOrigin.contains("*")) {
                throw new IllegalArgumentException("CORS_ALLOWED_ORIGIN must be one exact origin, not a pattern");
            }
        }
    }
}
