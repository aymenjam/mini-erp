package org.j2ra.minierp.gateway.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

/** Test only: a route filter that reads the correlation id from the Reactor context, like code deeper in the chain would. */
@TestConfiguration
public class ContextProbeConfig {

    public static final AtomicReference<Object> SEEN = new AtomicReference<>();

    @Bean
    GlobalFilter contextProbe() {
        return (exchange, chain) -> Mono.deferContextual(context -> {
            SEEN.set(context.getOrDefault("correlationId", null));
            return chain.filter(exchange);
        });
    }
}
