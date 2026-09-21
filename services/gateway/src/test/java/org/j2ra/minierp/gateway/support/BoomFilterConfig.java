package org.j2ra.minierp.gateway.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Mono;

/** Test only: a route filter that fails on the path ending with /boom, to prove that an unexpected error gives a clean 500. */
@TestConfiguration
public class BoomFilterConfig {

    public static final String SECRET = "secret internal detail jdbc:postgresql://db.internal:5432/erp";

    @Bean
    GlobalFilter boomFilter() {
        return (exchange, chain) -> exchange.getRequest().getURI().getPath().endsWith("/boom")
                ? Mono.error(new IllegalStateException(SECRET))
                : chain.filter(exchange);
    }
}
