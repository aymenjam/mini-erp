package org.j2ra.minierp.gateway.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.web.reactive.error.ErrorWebFluxAutoConfiguration;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;

/**
 * Turns every error of the gateway into Problem Details: 404 (no route), 503 (service not reachable),
 * 504 (service too slow) and 500 (anything else). It replaces the default handler of Spring Boot
 * (which is switched off when a bean of this type exists, see {@link ErrorWebFluxAutoConfiguration}).
 *
 * <p>The status comes from the type of the exception (and its causes), not from its text.
 */
@Component
@Order(-2)
class GatewayErrorHandler implements ErrorWebExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GatewayErrorHandler.class);
    // A cause chain is short. The limit protects against a chain that points to itself.
    private static final int MAX_CAUSE_DEPTH = 10;

    private final ProblemDetailsWriter writer;

    GatewayErrorHandler(ProblemDetailsWriter writer) {
        this.writer = writer;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable error) {
        if (exchange.getResponse().isCommitted()) {
            // Too late to write an answer. Let the next handler log it.
            return Mono.error(error);
        }
        HttpStatus status = statusFor(error);
        if (status.is5xxServerError()) {
            String id = exchange.getAttributeOrDefault(CorrelationIdWebFilter.ATTRIBUTE, "-");
            CorrelationLog.error(LOG, id, "Request failed with {}", status.value(), error);
        }
        return writer.write(exchange, status, detailFor(status));
    }

    static HttpStatus statusFor(Throwable error) {
        // Order matters. Netty's ConnectTimeoutException is also a ConnectException, so timeouts are checked first.
        if (hasCause(error, TimeoutException.class) || hasCause(error, io.netty.channel.ConnectTimeoutException.class)
                || hasCause(error, io.netty.handler.timeout.ReadTimeoutException.class)) {
            return HttpStatus.GATEWAY_TIMEOUT;
        }
        if (hasCause(error, ConnectException.class) || hasCause(error, UnknownHostException.class)) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }
        ResponseStatusException responseStatus = find(error, ResponseStatusException.class);
        if (responseStatus != null) {
            HttpStatusCode code = responseStatus.getStatusCode();
            if (code.is4xxClientError() || code.value() == 503 || code.value() == 504) {
                return HttpStatus.valueOf(code.value());
            }
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private static String detailFor(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> "No route matches this path.";
            case SERVICE_UNAVAILABLE -> "The service is not reachable.";
            case GATEWAY_TIMEOUT -> "The service did not answer in time.";
            case INTERNAL_SERVER_ERROR -> "An unexpected error occurred.";
            default -> "The request cannot be handled.";
        };
    }

    private static boolean hasCause(Throwable error, Class<? extends Throwable> type) {
        return find(error, type) != null;
    }

    private static <T extends Throwable> T find(Throwable error, Class<T> type) {
        Throwable current = error;
        for (int depth = 0; current != null && depth < MAX_CAUSE_DEPTH; depth++) {
            if (type.isInstance(current)) {
                return type.cast(current);
            }
            current = current.getCause();
        }
        return null;
    }
}
