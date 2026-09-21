package org.j2ra.minierp.gateway;

import org.j2ra.minierp.gateway.support.AbstractGatewayTest;
import org.j2ra.minierp.gateway.support.TestTokens;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.EntityExchangeResult;

import static org.assertj.core.api.Assertions.assertThat;

/** The gateway checks the token (signature, expiry, issuer) and routes. Roles are the business of the services. */
class GatewaySecurityTest extends AbstractGatewayTest {

    @Test
    @DisplayName("AC-3: no token gives 401, and the request does not reach the service")
    void no_token_gives_401() {
        client.get().uri("/api/v1/customers").exchange().expectStatus().isUnauthorized();
        assertThat(downstream.requests()).isEmpty();
    }

    @Test
    @DisplayName("AC-3: an expired token gives 401")
    void expired_token_gives_401() {
        client.get().uri("/api/v1/customers").header(HttpHeaders.AUTHORIZATION, bearer(TestTokens.expired("SALES")))
                .exchange().expectStatus().isUnauthorized();
        assertThat(downstream.requests()).isEmpty();
    }

    @Test
    @DisplayName("AC-3: a token signed with another key gives 401")
    void wrong_key_gives_401() {
        client.get().uri("/api/v1/customers").header(HttpHeaders.AUTHORIZATION, bearer(TestTokens.wrongKey("SALES")))
                .exchange().expectStatus().isUnauthorized();
        assertThat(downstream.requests()).isEmpty();
    }

    @Test
    @DisplayName("AC-5: the issuer is still checked when jwk-set-uri is set: a token from another issuer gives 401")
    void wrong_issuer_gives_401() {
        client.get().uri("/api/v1/customers").header(HttpHeaders.AUTHORIZATION, bearer(TestTokens.wrongIssuer("SALES")))
                .exchange().expectStatus().isUnauthorized();
        assertThat(downstream.requests()).isEmpty();
    }

    @Test
    @DisplayName("AC-3: text that is not a token gives 401")
    void garbage_token_gives_401() {
        client.get().uri("/api/v1/customers").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt")
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("AC-6: a valid token is proxied. The service gets Authorization and X-Correlation-Id")
    void valid_token_is_proxied_with_headers() {
        String token = TestTokens.valid("SALES");

        EntityExchangeResult<String> result = client.get().uri("/api/v1/customers")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).returnResult();

        assertThat(result.getResponseBody()).isEqualTo("{\"content\":[]}");
        assertThat(downstream.lastRequest().path()).isEqualTo("/api/v1/customers");
        assertThat(downstream.lastRequest().header("Authorization")).isEqualTo(bearer(token));
        assertThat(downstream.lastRequest().header("X-Correlation-Id"))
                .isNotBlank()
                .isEqualTo(result.getResponseHeaders().getFirst("X-Correlation-Id"));
    }

    @Test
    @DisplayName("AC-3: a 403 from the service passes through unchanged (status, type and body)")
    void forbidden_from_service_passes_through() {
        String body = "{\"type\":\"about:blank\",\"title\":\"Forbidden\",\"status\":403,"
                + "\"detail\":\"You do not have the role needed for this request.\",\"instance\":\"/api/v1/customers\"}";
        downstream.respond(403, "application/problem+json", body);

        client.get().uri("/api/v1/customers").header(HttpHeaders.AUTHORIZATION, bearer(TestTokens.valid("WAREHOUSE")))
                .exchange()
                .expectStatus().isForbidden()
                .expectHeader().contentType("application/problem+json")
                .expectBody(String.class).isEqualTo(body);
    }

    @Test
    @DisplayName("ADR-010 (CVE-2025-41253): no gateway actuator endpoint is reachable, with or without a token")
    void gateway_actuator_is_not_exposed() {
        for (String path : new String[]{"/actuator/gateway/routes", "/actuator/gateway", "/actuator/env", "/actuator/beans"}) {
            client.get().uri(path).exchange().expectStatus().isUnauthorized();
            client.get().uri(path).header(HttpHeaders.AUTHORIZATION, bearer(TestTokens.valid("ADMIN")))
                    .exchange().expectStatus().isNotFound();
        }
    }
}
