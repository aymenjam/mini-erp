package org.j2ra.minierp.gateway.config;

import org.j2ra.minierp.gateway.web.ProblemDetailsAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.savedrequest.NoOpServerRequestCache;
import org.springframework.web.cors.reactive.CorsConfigurationSource;

/**
 * Security of the gateway: it checks the JWT (signature, expiry, issuer) and nothing else.
 * Roles are not checked here. The services check them, and their answers pass through unchanged.
 */
@Configuration
@EnableWebFluxSecurity
class SecurityConfig {

    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http, Environment environment,
                                                  CorsConfigurationSource corsConfigurationSource,
                                                  ProblemDetailsAuthenticationEntryPoint entryPoint) {
        boolean dev = environment.acceptsProfiles(Profiles.of("dev"));
        return http
                .authorizeExchange(exchanges -> {
                    exchanges.pathMatchers("/actuator/health/**").permitAll();
                    // A browser cannot send a token when it loads Swagger UI, so these paths are open. Only in dev.
                    // Two locks: here (no permitAll outside dev) and DevRoutesConfig (no route outside dev).
                    if (dev) {
                        exchanges.pathMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll();
                    }
                    // An unknown path gives 401 without a token, and 404 with a valid one.
                    exchanges.anyExchange().authenticated();
                })
                // The CORS filter runs before authentication: a preflight has no token and must still be answered.
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(entryPoint))
                .exceptionHandling(errors -> errors.authenticationEntryPoint(entryPoint))
                // Stateless API with a bearer token in a header: no cookie, so no CSRF risk, no session, no saved request.
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .requestCache(cache -> cache.requestCache(NoOpServerRequestCache.getInstance()))
                .build();
    }
}
