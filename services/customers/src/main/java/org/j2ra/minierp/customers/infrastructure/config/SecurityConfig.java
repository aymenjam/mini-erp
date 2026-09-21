package org.j2ra.minierp.customers.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/** Composition root of security: resource server, public health check, every other request needs a valid token. */
@Configuration
@EnableMethodSecurity
class SecurityConfig {

    // Same switch as springdoc. It is true only with the profile dev (application-dev.yaml), and false if the property is missing.
    private static final String API_DOCS_ENABLED = "${springdoc.api-docs.enabled:false}";

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ProblemDetailsSecurityHandlers handlers,
                                            @Value(API_DOCS_ENABLED) boolean apiDocsEnabled) throws Exception {
        return http
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/actuator/health/**").permitAll();
                    // Swagger UI cannot send a token before it is loaded, so its pages must be open. We open them only in dev.
                    // Two locks: here (no permitAll outside dev) and in springdoc (the URLs do not exist outside dev).
                    if (apiDocsEnabled) {
                        auth.requestMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll();
                    }
                    auth.anyRequest().authenticated();
                })
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(handlers)
                        .accessDeniedHandler(handlers))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(handlers)
                        .accessDeniedHandler(handlers))
                // Stateless API with a bearer token in a header: no cookie, so no CSRF risk and no session.
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .build();
    }

    @Bean
    ProblemDetailsSecurityHandlers problemDetailsSecurityHandlers(ObjectMapper objectMapper) {
        return new ProblemDetailsSecurityHandlers(objectMapper);
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new RealmRolesConverter());
        return converter;
    }
}
