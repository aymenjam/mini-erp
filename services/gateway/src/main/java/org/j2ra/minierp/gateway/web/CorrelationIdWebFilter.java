package org.j2ra.minierp.gateway.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Creates or keeps X-Correlation-Id. It runs before security, so the 401 answers have the id too.
 * The id goes to the response, to the service behind the gateway, and into one log line per request.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdWebFilter implements WebFilter {

    public static final String HEADER = "X-Correlation-Id";
    /** Name of the exchange attribute and of the Reactor context key. Error handlers read the id from there. */
    public static final String ATTRIBUTE = "correlationId";

    // Same rule as services/customers. The value goes into log lines: only safe characters, so nobody can inject a fake log line.
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9-]{1,64}");

    private static final Logger LOG = LoggerFactory.getLogger(CorrelationIdWebFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String incoming = exchange.getRequest().getHeaders().getFirst(HEADER);
        String id = incoming != null && SAFE_ID.matcher(incoming).matches() ? incoming : UUID.randomUUID().toString();

        // The gateway copies the request headers to the service. A replaced (unsafe) value must not go through.
        ServerHttpRequest request = exchange.getRequest().mutate().headers(headers -> headers.set(HEADER, id)).build();
        ServerWebExchange withId = exchange.mutate().request(request).build();
        // The attribute map is shared with the original exchange, so error handlers (they get the original) can read it.
        withId.getAttributes().put(ATTRIBUTE, id);

        ServerHttpResponse response = withId.getResponse();
        // Last moment before the answer is sent: the status is known, and "set" keeps one value even if the service also sent the header.
        response.beforeCommit(() -> {
            response.getHeaders().set(HEADER, id);
            CorrelationLog.info(LOG, id, "{} {} -> {}", request.getMethod(), request.getURI().getRawPath(), response.getStatusCode());
            return Mono.empty();
        });

        return chain.filter(withId).contextWrite(context -> context.put(ATTRIBUTE, id));
    }
}
