package org.j2ra.minierp.gateway.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * The gateway on a random port, with fake values only: a fake Keycloak key set, a fake customers, and a short response timeout.
 * The values have the names of the real environment variables (.env.example).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractGatewayTest {

    protected static final String ALLOWED_ORIGIN = "http://localhost:4200";

    @DynamicPropertySource
    static void gatewayProperties(DynamicPropertyRegistry registry) {
        registry.add("ISSUER_URI", () -> TestTokens.ISSUER);
        registry.add("JWK_SET_URI", TestTokens::jwkSetUri);
        registry.add("CUSTOMERS_URI", () -> FakeDownstream.INSTANCE.uri().toString());
        registry.add("CORS_ALLOWED_ORIGIN", () -> ALLOWED_ORIGIN);
        // Short, so the timeout test is fast. The fake downstream sleeps longer than this.
        registry.add("GATEWAY_RESPONSE_TIMEOUT_MS", () -> "1000");
    }

    @Autowired
    protected WebTestClient client;

    protected final FakeDownstream downstream = FakeDownstream.INSTANCE;

    @BeforeEach
    void resetDownstream() {
        downstream.reset();
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }
}
