package org.j2ra.minierp.gateway;

import org.j2ra.minierp.gateway.support.AbstractGatewayTest;
import org.j2ra.minierp.gateway.support.FakeDownstream;
import org.j2ra.minierp.gateway.support.TestTokens;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/** AC-13: CORS is answered by the gateway, before the token check, for the allowed origin only. */
class CorsTest {

    private static final String OTHER_ORIGIN = "https://evil.example.test";

    private static EntityExchangeResult<byte[]> preflight(WebTestClient client, String origin) {
        return client.options().uri("/api/v1/customers")
                .header(HttpHeaders.ORIGIN, origin)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type,accept,x-correlation-id")
                .exchange().expectBody().returnResult();
    }

    @Nested
    class AllowedOriginLocalhost4200 extends AbstractGatewayTest {

        @Test
        @DisplayName("AC-13: a preflight from the allowed origin is answered without a token, with all methods and headers")
        void allowed_preflight() {
            EntityExchangeResult<byte[]> result = preflight(client, ALLOWED_ORIGIN);

            assertThat(result.getStatus().is2xxSuccessful()).isTrue();
            HttpHeaders headers = result.getResponseHeaders();
            assertThat(headers.getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo(ALLOWED_ORIGIN);
            assertThat(headers.getAccessControlAllowMethods()).extracting(Object::toString)
                    .containsExactlyInAnyOrder("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
            // Spring answers with the names as the browser wrote them (lower case). Header names are case-insensitive.
            assertThat(headers.getAccessControlAllowHeaders()).usingElementComparator(String.CASE_INSENSITIVE_ORDER)
                    .containsExactlyInAnyOrder("Authorization", "Content-Type", "Accept", "X-Correlation-Id");
            assertThat(headers.getAccessControlMaxAge()).isEqualTo(3600L);
            assertThat(headers.getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isNull();
            // The preflight is finished at the gateway. The service does not see it.
            assertThat(downstream.requests()).isEmpty();
        }

        @Test
        @DisplayName("AC-13: a preflight for a method that is not allowed is refused")
        void method_not_allowed_is_refused() {
            EntityExchangeResult<byte[]> result = client.options().uri("/api/v1/customers")
                    .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "TRACE")
                    .exchange().expectBody().returnResult();

            assertThat(result.getStatus().value()).isEqualTo(403);
            assertThat(result.getResponseHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
        }

        @Test
        @DisplayName("AC-13: a preflight that asks for a header that is not allowed is refused")
        void header_not_allowed_is_refused() {
            EntityExchangeResult<byte[]> result = client.options().uri("/api/v1/customers")
                    .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "x-secret-header")
                    .exchange().expectBody().returnResult();

            assertThat(result.getStatus().value()).isEqualTo(403);
        }

        @Test
        @DisplayName("AC-13: a preflight from another origin is refused, and it does not reach the service")
        void other_origin_is_refused() {
            EntityExchangeResult<byte[]> result = preflight(client, OTHER_ORIGIN);

            assertThat(result.getStatus().value()).isEqualTo(403);
            assertThat(result.getResponseHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
            assertThat(downstream.requests()).isEmpty();
        }

        @Test
        @DisplayName("AC-13: the real answer has the allowed origin, and the browser can read X-Correlation-Id")
        void real_answer_has_cors_headers() {
            EntityExchangeResult<byte[]> result = client.get().uri("/api/v1/customers")
                    .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                    .header(HttpHeaders.AUTHORIZATION, bearer(TestTokens.valid("SALES")))
                    .exchange().expectStatus().isOk().expectBody().returnResult();

            HttpHeaders headers = result.getResponseHeaders();
            assertThat(headers.get(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).containsExactly(ALLOWED_ORIGIN);
            assertThat(headers.getAccessControlExposeHeaders()).containsExactly("X-Correlation-Id");
            assertThat(headers.getFirst("X-Correlation-Id")).isNotBlank();
            assertThat(headers.getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isNull();
        }

        @Test
        @DisplayName("AC-13: a 401 answer to the allowed origin has the CORS headers too, so the browser can show the error")
        void unauthorized_answer_has_cors_headers() {
            EntityExchangeResult<byte[]> result = client.get().uri("/api/v1/customers")
                    .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                    .exchange().expectStatus().isUnauthorized().expectBody().returnResult();

            assertThat(result.getResponseHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo(ALLOWED_ORIGIN);
            assertThat(result.getResponseHeaders().getAccessControlExposeHeaders()).containsExactly("X-Correlation-Id");
        }
    }

    /** The origin comes from CORS_ALLOWED_ORIGIN, not from the code. */
    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    class OriginFromTheVariable {

        private static final String ORIGIN = "https://erp.example.test";

        @DynamicPropertySource
        static void gatewayProperties(DynamicPropertyRegistry registry) {
            registry.add("ISSUER_URI", () -> TestTokens.ISSUER);
            registry.add("JWK_SET_URI", TestTokens::jwkSetUri);
            registry.add("CUSTOMERS_URI", () -> FakeDownstream.INSTANCE.uri().toString());
            registry.add("CORS_ALLOWED_ORIGIN", () -> ORIGIN);
        }

        @Autowired
        private WebTestClient client;

        @Test
        @DisplayName("AC-13: the origin of the variable is allowed, and http://localhost:4200 is not")
        void origin_comes_from_the_variable() {
            assertThat(preflight(client, ORIGIN).getResponseHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo(ORIGIN);

            EntityExchangeResult<byte[]> refused = preflight(client, "http://localhost:4200");
            assertThat(refused.getStatus().value()).isEqualTo(403);
            assertThat(refused.getResponseHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
        }
    }
}
