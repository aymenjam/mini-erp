package org.j2ra.minierp.gateway;

import org.j2ra.minierp.gateway.support.AbstractGatewayTest;
import org.j2ra.minierp.gateway.support.TestTokens;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/** AC-16: OpenAPI and Swagger UI of customers are reachable without a token in the profile dev only. */
class DevRoutesTest {

    private static final String[] DOC_PATHS = {"/v3/api-docs", "/v3/api-docs/swagger-config", "/swagger-ui.html", "/swagger-ui/index.html"};

    @Nested
    @ActiveProfiles("dev")
    class WithProfileDev extends AbstractGatewayTest {

        @LocalServerPort
        private int port;

        @Autowired
        private Environment environment;

        @Test
        @DisplayName("AC-16: with dev, the documentation paths pass without a token and reach customers")
        void documentation_paths_are_open() {
            for (String path : DOC_PATHS) {
                downstream.reset();
                client.get().uri(path).exchange().expectStatus().isOk();
                assertThat(downstream.lastRequest().path()).as("path %s at the service", path).isEqualTo(path);
            }
        }

        @Test
        @DisplayName("AC-16 / AC-4: with dev, every other path still needs a token")
        void other_paths_need_a_token() {
            for (String path : new String[]{"/api/v1/customers", "/api/v1/customers/1", "/v3/other", "/swagger", "/swagger-ui-x", "/actuator/info", "/anything"}) {
                client.get().uri(path).exchange().expectStatus().isUnauthorized();
            }
            assertThat(downstream.requests()).isEmpty();
        }

        @Test
        @DisplayName("AC-16: with dev, the API itself is still routed with a token")
        void api_is_routed() {
            client.get().uri("/api/v1/customers").header(HttpHeaders.AUTHORIZATION, bearer(TestTokens.valid("SALES")))
                    .exchange().expectStatus().isOk();
        }

        @Test
        @DisplayName("AC-16: with dev, the gateway tells customers its own address (X-Forwarded-*), so Swagger UI shows the gateway as the server")
        void forwarded_headers_reach_customers_from_a_local_caller() {
            client.get().uri("/v3/api-docs").exchange().expectStatus().isOk();

            assertThat(downstream.lastRequest().header("X-Forwarded-Host")).isEqualTo("localhost:" + port);
            assertThat(downstream.lastRequest().header("X-Forwarded-Proto")).isEqualTo("http");
        }

        @Test
        @DisplayName("ADR-010: the dev value of trusted-proxies is the narrowest that works: IPv4 and IPv6 loopback, nothing else")
        void trusted_proxies_is_loopback_only() {
            String pattern = environment.getProperty("spring.cloud.gateway.server.webflux.trusted-proxies");

            assertThat(pattern).isEqualTo("127\\.0\\.0\\.1|0:0:0:0:0:0:0:1");
            assertThat("127.0.0.1").matches(pattern);
            assertThat("0:0:0:0:0:0:0:1").matches(pattern);
            assertThat("127.0.0.10").doesNotMatch(pattern);
            assertThat("10.0.0.5").doesNotMatch(pattern);
            assertThat("192.168.1.10").doesNotMatch(pattern);
        }
    }

    @Nested
    class WithoutProfileDev extends AbstractGatewayTest {

        @Test
        @DisplayName("ADR-010: without dev, X-Forwarded-* handling is off: none is added, and one sent by the caller is dropped")
        void forwarded_headers_are_off_by_default() {
            client.get().uri("/api/v1/customers").header(HttpHeaders.AUTHORIZATION, bearer(TestTokens.valid("SALES")))
                    .header("X-Forwarded-Host", "evil.example.test").header("X-Forwarded-For", "6.6.6.6")
                    .exchange().expectStatus().isOk();

            assertThat(downstream.lastRequest().header("X-Forwarded-Host")).isNull();
            assertThat(downstream.lastRequest().header("X-Forwarded-For")).isNull();
            assertThat(downstream.lastRequest().header("X-Forwarded-Proto")).isNull();
        }

        @Test
        @DisplayName("AC-16: without dev, the three documentation paths give 401 without a token")
        void documentation_paths_need_a_token() {
            for (String path : DOC_PATHS) {
                client.get().uri(path).exchange().expectStatus().isUnauthorized();
            }
            assertThat(downstream.requests()).isEmpty();
        }

        @Test
        @DisplayName("AC-16: without dev, the documentation paths give 404 with a token (no route), and customers is not called")
        void documentation_paths_have_no_route() {
            for (String path : DOC_PATHS) {
                client.get().uri(path).header(HttpHeaders.AUTHORIZATION, bearer(TestTokens.valid("ADMIN")))
                        .exchange().expectStatus().isNotFound();
            }
            assertThat(downstream.requests()).isEmpty();
        }
    }
}
