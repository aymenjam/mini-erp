package org.j2ra.minierp.customers.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.SupplierJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;

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

    // Spring Boot's own auto-configuration (servlet stack) creates two competing JwtDecoder beans
    // when both issuer-uri and jwk-set-uri are set (unlike the gateway's reactive stack, which
    // combines them into one). This single bean replaces both: keys from jwk-set-uri when it is
    // set (container mode, ADR-010 addendum: the container reaches Keycloak on the compose
    // network, not on localhost), issuer still checked against issuer-uri either way. Falls back to
    // plain OIDC discovery (issuer-uri only) when jwk-set-uri is not set, exactly as before (dev
    // mode; TestSecurityBeans replaces this bean entirely in tests, so this method never runs there).
    // SupplierJwtDecoder: the discovery call happens on first use, not at startup (same laziness as
    // Boot's own issuer-uri-only autoconfiguration; a test proves it, and it keeps startup fast and
    // independent of Keycloak being up yet, like jwk-set-uri already is).
    @Bean
    @ConditionalOnMissingBean(JwtDecoder.class)
    JwtDecoder jwtDecoder(@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri,
                          @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:}") String jwkSetUri) {
        if (StringUtils.hasText(jwkSetUri)) {
            NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
            decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuerUri));
            return decoder;
        }
        return new SupplierJwtDecoder(() -> JwtDecoders.fromOidcIssuerLocation(issuerUri));
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new RealmRolesConverter());
        return converter;
    }
}
