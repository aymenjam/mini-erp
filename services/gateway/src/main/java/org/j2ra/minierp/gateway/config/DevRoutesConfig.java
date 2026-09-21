package org.j2ra.minierp.gateway.config;

import org.j2ra.minierp.gateway.GatewayProperties;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * OpenAPI document and Swagger UI of customers, through the gateway. Only with the profile dev.
 * Outside dev there is no route, and SecurityConfig opens no path: an unknown path gives 401 (no token) or 404 (with a token).
 */
@Configuration
@Profile("dev")
class DevRoutesConfig {

    @Bean
    RouteLocator customersOpenApiRoute(RouteLocatorBuilder builder, GatewayProperties properties) {
        return builder.routes()
                .route("customers-openapi", route -> route
                        .path("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**")
                        .uri(properties.customersUri()))
                .build();
    }
}
