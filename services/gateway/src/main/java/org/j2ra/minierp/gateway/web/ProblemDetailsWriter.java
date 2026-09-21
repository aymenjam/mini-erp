package org.j2ra.minierp.gateway.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * Writes the errors that the gateway makes itself as Problem Details (RFC 9457): same format as the services.
 * The text is fixed and general. The real cause goes to the log, never into the answer.
 */
@Component
class ProblemDetailsWriter {

    private static final String FALLBACK_BODY = "{\"type\":\"about:blank\",\"title\":\"Internal Server Error\",\"status\":500}";

    private final ObjectMapper objectMapper;

    ProblemDetailsWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    Mono<Void> write(ServerWebExchange exchange, HttpStatus status, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setInstance(instanceOf(exchange));

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(problem);
        } catch (JsonProcessingException e) {
            // Cannot happen for a ProblemDetail, but an error handler must never fail because of its own error.
            bytes = FALLBACK_BODY.getBytes(StandardCharsets.UTF_8);
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        response.getHeaders().setContentLength(bytes.length);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    /** The path only, without the query string (it can hold private values). */
    private static URI instanceOf(ServerWebExchange exchange) {
        String path = exchange.getRequest().getURI().getRawPath();
        try {
            // "//x" would be read as a host name, so we keep one slash only.
            return URI.create("/" + path.replaceFirst("^/+", ""));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
