package org.j2ra.minierp.gateway;

import org.j2ra.minierp.gateway.support.AbstractGatewayTest;
import org.j2ra.minierp.gateway.support.BoomFilterConfig;
import org.j2ra.minierp.gateway.support.TestTokens;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AC-21: the errors the gateway makes itself are application/problem+json, have X-Correlation-Id
 * and show no internal detail (no exception name, no class, no address, no secret).
 */
class GatewayErrorsTest {

    /** Words that would show an internal detail. */
    private static final String[] INTERNAL = {"Exception", "org.springframework", "java.", "io.netty", "reactor", "Jwt", "jdbc",
            "127.0.0.1", "localhost", "secret", "at org.", "stackTrace", "trace"};

    private static void assertProblem(EntityExchangeResult<String> result, int status) {
        assertThat(result.getStatus().value()).isEqualTo(status);
        assertThat(result.getResponseHeaders().getContentType()).hasToString("application/problem+json");
        assertThat(result.getResponseHeaders().getFirst("X-Correlation-Id")).isNotBlank();
        String body = result.getResponseBody();
        assertThat(body).contains("\"status\":" + status).contains("\"title\":").contains("\"detail\":");
        for (String word : INTERNAL) {
            assertThat(body).as("the body must not show '%s'", word).doesNotContain(word);
        }
    }

    @Nested
    @Import(BoomFilterConfig.class)
    class WithCustomersUp extends AbstractGatewayTest {

        @Test
        @DisplayName("AC-21: no token gives 401 Problem Details")
        void no_token_gives_401() {
            EntityExchangeResult<String> result = client.get().uri("/api/v1/customers").exchange().expectBody(String.class).returnResult();
            assertProblem(result, 401);
            assertThat(result.getResponseHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE)).isEqualTo("Bearer");
        }

        @Test
        @DisplayName("AC-21: an invalid token, an expired token and a wrong issuer give 401 Problem Details, without the reason")
        void bad_tokens_give_401() {
            for (String token : new String[]{"not-a-jwt", TestTokens.expired("SALES"), TestTokens.wrongKey("SALES"), TestTokens.wrongIssuer("SALES")}) {
                EntityExchangeResult<String> result = client.get().uri("/api/v1/customers")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)).exchange().expectBody(String.class).returnResult();
                assertProblem(result, 401);
                // The header tells "invalid token" and nothing more (no error_description).
                assertThat(result.getResponseHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE)).isEqualTo("Bearer error=\"invalid_token\"");
            }
        }

        @Test
        @DisplayName("AC-21: an unknown route with a valid token gives 404 Problem Details")
        void unknown_route_gives_404() {
            EntityExchangeResult<String> result = client.get().uri("/api/v1/nothing-here")
                    .header(HttpHeaders.AUTHORIZATION, bearer(TestTokens.valid("ADMIN"))).exchange().expectBody(String.class).returnResult();
            assertProblem(result, 404);
            assertThat(result.getResponseBody()).contains("\"instance\":\"/api/v1/nothing-here\"");
        }

        @Test
        @DisplayName("AC-21: a service that is too slow gives 504 Problem Details")
        void slow_service_gives_504() {
            downstream.respondAfter(Duration.ofSeconds(4));
            EntityExchangeResult<String> result = client.get().uri("/api/v1/customers")
                    .header(HttpHeaders.AUTHORIZATION, bearer(TestTokens.valid("SALES"))).exchange().expectBody(String.class).returnResult();
            assertProblem(result, 504);
        }

        @Test
        @DisplayName("AC-21: an unexpected error gives 500 Problem Details, and the message stays in the log")
        void unexpected_error_gives_500() {
            EntityExchangeResult<String> result = client.get().uri("/api/v1/customers/boom")
                    .header(HttpHeaders.AUTHORIZATION, bearer(TestTokens.valid("SALES"))).exchange().expectBody(String.class).returnResult();
            assertProblem(result, 500);
            assertThat(result.getResponseBody()).doesNotContain(BoomFilterConfig.SECRET);
        }
    }

    /** The service refuses the connection: its port is closed. */
    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    class WithCustomersDown {

        @DynamicPropertySource
        static void gatewayProperties(DynamicPropertyRegistry registry) throws IOException {
            int closedPort;
            try (ServerSocket socket = new ServerSocket(0)) {
                closedPort = socket.getLocalPort();
            }
            registry.add("ISSUER_URI", () -> TestTokens.ISSUER);
            registry.add("JWK_SET_URI", TestTokens::jwkSetUri);
            registry.add("CUSTOMERS_URI", () -> "http://127.0.0.1:" + closedPort);
            registry.add("CORS_ALLOWED_ORIGIN", () -> "http://localhost:4200");
        }

        @Autowired
        private WebTestClient client;

        @Test
        @DisplayName("AC-21: customers down gives 503 Problem Details (504 is for a timeout)")
        void service_down_gives_503() {
            EntityExchangeResult<String> result = client.get().uri("/api/v1/customers")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestTokens.valid("SALES"))
                    .exchange().expectBody(String.class).returnResult();
            assertProblem(result, 503);
        }
    }
}
