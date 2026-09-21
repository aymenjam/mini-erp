package org.j2ra.minierp.gateway.config;

import org.j2ra.minierp.gateway.GatewayProperties;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The routes of the gateway. Java DSL, so the compiler checks them. The Authorization and X-Correlation-Id headers go through. */
@Configuration
class RoutesConfig {

    @Bean
    RouteLocator customersApiRoute(RouteLocatorBuilder builder, GatewayProperties properties) {
        return builder.routes()
                .route("customers-api", route -> route
                        .path("/api/v1/customers/**")
                        .uri(properties.customersUri()))
                .build();
    }
}
