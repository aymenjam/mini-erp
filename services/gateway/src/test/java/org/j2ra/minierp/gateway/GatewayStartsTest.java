package org.j2ra.minierp.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The application starts with fake values, and it does not start without a value that has no default. */
class GatewayStartsTest {

    /** The four values without a default outside the profile dev. All fake: nothing depends on the .env of a developer. */
    private static final Map<String, String> FAKE_VALUES = Map.of(
            "ISSUER_URI", "http://issuer.test/realms/mini-erp",
            "JWK_SET_URI", "http://issuer.test/realms/mini-erp/protocol/openid-connect/certs",
            "CUSTOMERS_URI", "http://customers.test:8091",
            "CORS_ALLOWED_ORIGIN", "http://localhost:4200");

    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
            "ISSUER_URI=http://issuer.test/realms/mini-erp",
            "JWK_SET_URI=http://issuer.test/realms/mini-erp/protocol/openid-connect/certs",
            "CUSTOMERS_URI=http://customers.test:8091",
            "CORS_ALLOWED_ORIGIN=http://localhost:4200"})
    @Nested
    class WithFakeValues {

        @Autowired
        private WebTestClient client;

        @Test
        @DisplayName("The context starts and /actuator/health is UP without a token")
        void health_is_up_and_public() {
            client.get().uri("/actuator/health").exchange()
                    .expectStatus().isOk()
                    .expectBody().jsonPath("$.status").isEqualTo("UP");
        }

        @Test
        @DisplayName("Any other path needs a token")
        void other_paths_need_a_token() {
            client.get().uri("/api/v1/customers").exchange().expectStatus().isUnauthorized();
        }

        @Test
        @DisplayName("AC-8: the answer has X-Correlation-Id, also when it is a 401")
        void answer_has_a_correlation_id() {
            client.get().uri("/api/v1/customers").exchange()
                    .expectHeader().exists("X-Correlation-Id");
        }
    }

    @ParameterizedTest(name = "without {0}")
    @ValueSource(strings = {"ISSUER_URI", "JWK_SET_URI", "CUSTOMERS_URI", "CORS_ALLOWED_ORIGIN"})
    @DisplayName("No default on purpose: without this variable the start fails at once")
    void start_fails_without(String missing) {
        Map<String, Object> others = new LinkedHashMap<>(FAKE_VALUES);
        others.remove(missing);

        SpringApplicationBuilder builder = new SpringApplicationBuilder(GatewayApplication.class)
                .web(WebApplicationType.REACTIVE)
                .environment(new EmptyEnvironment())
                .properties(others)
                .properties("server.port=0");

        assertThatThrownBy(builder::run).hasStackTraceContaining(missing);
    }

    /** No system properties and no environment variables. Boot still adds application.yaml and the properties above. */
    private static class EmptyEnvironment extends StandardEnvironment {
        @Override
        protected void customizePropertySources(MutablePropertySources propertySources) {
            // On purpose empty: a variable of the machine that runs the test must not fill the gap.
        }
    }
}
