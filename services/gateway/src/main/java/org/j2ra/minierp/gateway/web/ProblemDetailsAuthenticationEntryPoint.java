package org.j2ra.minierp.gateway.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** 401 for a missing, invalid or expired token, or a wrong issuer. One answer for all cases: the client learns nothing about why. */
@Component
public class ProblemDetailsAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {

    private static final Logger LOG = LoggerFactory.getLogger(ProblemDetailsAuthenticationEntryPoint.class);

    private final ProblemDetailsWriter writer;

    ProblemDetailsAuthenticationEntryPoint(ProblemDetailsWriter writer) {
        this.writer = writer;
    }

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException exception) {
        boolean tokenWasSent = exception instanceof OAuth2AuthenticationException;
        if (tokenWasSent) {
            // The reason (expired, bad signature, wrong issuer) is useful for the operator, so it goes to the log only.
            CorrelationLog.info(LOG, correlationId(exchange), "Token rejected: {}", exception.getMessage());
        }
        // RFC 6750: tell the client that a Bearer token is expected. No error description (it holds internal detail).
        exchange.getResponse().getHeaders().set(HttpHeaders.WWW_AUTHENTICATE,
                tokenWasSent ? "Bearer error=\"invalid_token\"" : "Bearer");
        return writer.write(exchange, HttpStatus.UNAUTHORIZED, "A valid access token is required.");
    }

    private static String correlationId(ServerWebExchange exchange) {
        return exchange.getAttributeOrDefault(CorrelationIdWebFilter.ATTRIBUTE, "-");
    }
}
