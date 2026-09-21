package org.j2ra.minierp.chaintest;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spec 0.4, AC-6: the whole backend chain with real parts: Keycloak, PostgreSQL, customers and gateway.
 * The chain starts once. The tests do not depend on each other: each one gets its own token and its own requests.
 */
@ExtendWith(LogTailOnFailure.class)
class ChainTest {

    private static final String API = "/api/v1/customers";
    private static final String ORIGIN = ChainEnvironment.ALLOWED_ORIGIN;
    private static final String OTHER_ORIGIN = "https://evil.example.test";
    private static final String CORRELATION = "X-Correlation-Id";
    private static final Duration LOG_DEADLINE = Duration.ofSeconds(20);

    private static ChainEnvironment chain;

    @BeforeAll
    static void startChain() {
        chain = ChainEnvironment.start();
    }

    @AfterAll
    static void stopChain() {
        if (chain != null) {
            chain.close();
        }
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static String gateway(String path) {
        return chain.gatewayUrl() + path;
    }

    private static String customers(String path) {
        return chain.customersUrl() + path;
    }

    // --- AC-6 and AC-3: the answers of the chain ---

    @Test
    @DisplayName("AC-6: sales gets 200 with an empty customer page through the gateway")
    void sales_gets_the_empty_list() {
        Http.Response response = Http.get(gateway(API), "Authorization", bearer(chain.token("sales")));

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.contentType()).startsWith("application/json");
        assertThat(JsonPath.<List<?>>read(response.body(), "$.content")).isEmpty();
        assertThat(JsonPath.<Integer>read(response.body(), "$.totalElements")).isZero();
        assertThat(JsonPath.<Integer>read(response.body(), "$.size")).isEqualTo(20);
    }

    @Test
    @DisplayName("AC-6 / AC-3: no token gets 401 Problem Details from the gateway")
    void no_token_gets_401() {
        Http.Response response = Http.get(gateway(API));

        assertThat(response.status()).isEqualTo(401);
        assertThat(response.contentType()).startsWith("application/problem+json");
        assertThat(JsonPath.<Integer>read(response.body(), "$.status")).isEqualTo(401);
        assertThat(response.header(CORRELATION)).isNotBlank();
    }

    @Test
    @DisplayName("AC-6 / AC-3: warehouse gets 403 from customers, and the gateway passes the Problem Details on unchanged")
    void warehouse_gets_403_unchanged() {
        String token = chain.token("warehouse");

        Http.Response direct = Http.get(customers(API), "Authorization", bearer(token));
        Http.Response viaGateway = Http.get(gateway(API), "Authorization", bearer(token));

        assertThat(direct.status()).isEqualTo(403);
        assertThat(viaGateway.status()).isEqualTo(direct.status());
        assertThat(viaGateway.contentType()).startsWith("application/problem+json").isEqualTo(direct.contentType());
        // Compare the stable fields, not the bytes (an id or a time could differ between two answers).
        for (String field : List.of("type", "title", "status", "detail")) {
            assertThat(JsonPath.<Object>read(viaGateway.body(), "$." + field)).as("field %s", field)
                    .isEqualTo(JsonPath.<Object>read(direct.body(), "$." + field));
        }
        // The gateway did not rewrite the body: same set of fields, nothing added, nothing removed.
        Map<String, Object> directBody = JsonPath.read(direct.body(), "$");
        Map<String, Object> gatewayBody = JsonPath.read(viaGateway.body(), "$");
        assertThat(gatewayBody.keySet()).isEqualTo(directBody.keySet());
        assertThat(JsonPath.<String>read(viaGateway.body(), "$.title")).isEqualTo("Forbidden");
    }

    // --- AC-4 and AC-5: tokens and issuers ---

    @Test
    @DisplayName("AC-4: a direct call to customers without a token gets 401 (the service does not trust the gateway alone)")
    void direct_call_without_token_gets_401() {
        Http.Response response = Http.get(customers(API));

        assertThat(response.status()).isEqualTo(401);
        assertThat(response.contentType()).startsWith("application/problem+json");
    }

    @Test
    @DisplayName("AC-5: a token of the expected issuer is accepted by the gateway and by customers")
    void token_of_the_expected_issuer_is_accepted() {
        String token = chain.token("sales");

        assertThat(KeycloakSupport.issuerOf(token)).isEqualTo(chain.expectedIssuer());
        assertThat(Http.get(gateway(API), "Authorization", bearer(token)).status()).isEqualTo(200);
        assertThat(Http.get(customers(API), "Authorization", bearer(token)).status()).isEqualTo(200);
    }

    @Test
    @DisplayName("AC-5: a token of another issuer (same Keycloak, same keys, other address) is rejected by the gateway and by customers")
    void token_of_another_issuer_is_rejected() {
        String token = chain.tokenFromOtherIssuer("sales");

        // The signature is right (Keycloak signed it), so only the issuer can be the reason for the 401.
        assertThat(KeycloakSupport.issuerOf(token)).isNotEqualTo(chain.expectedIssuer());
        assertThat(Http.get(gateway(API), "Authorization", bearer(token)).status()).isEqualTo(401);
        assertThat(Http.get(customers(API), "Authorization", bearer(token)).status()).isEqualTo(401);
    }

    // --- AC-13: CORS ---

    @Test
    @DisplayName("AC-13: a preflight from the allowed origin is answered by the gateway without a token")
    void preflight_from_the_allowed_origin() {
        Http.Response response = Http.options(gateway(API), "Origin", ORIGIN, "Access-Control-Request-Method", "GET",
                "Access-Control-Request-Headers", "authorization,content-type,accept,x-correlation-id");

        assertThat(response.status()).isBetween(200, 204);
        assertThat(response.header("Access-Control-Allow-Origin")).isEqualTo(ORIGIN);
        assertThat(response.header("Access-Control-Allow-Methods")).contains("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
        assertThat(response.header("Access-Control-Allow-Headers").toLowerCase()).contains("authorization", "x-correlation-id");
        assertThat(response.header("Access-Control-Max-Age")).isEqualTo("3600");
        assertThat(response.header("Access-Control-Allow-Credentials")).isNull();
    }

    @Test
    @DisplayName("AC-13: the real answer has the CORS headers, and the browser can read X-Correlation-Id")
    void real_answer_has_cors_headers() {
        Http.Response response = Http.get(gateway(API), "Origin", ORIGIN, "Authorization", bearer(chain.token("sales")));

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.headerValues("Access-Control-Allow-Origin")).containsExactly(ORIGIN);
        assertThat(response.header("Access-Control-Expose-Headers")).containsIgnoringCase(CORRELATION);
    }

    @Test
    @DisplayName("AC-13: a preflight from another origin is refused")
    void preflight_from_another_origin_is_refused() {
        Http.Response response = Http.options(gateway(API), "Origin", OTHER_ORIGIN, "Access-Control-Request-Method", "GET");

        assertThat(response.status()).isEqualTo(403);
        assertThat(response.header("Access-Control-Allow-Origin")).isNull();
    }

    @Test
    @DisplayName("AC-13: customers sets no CORS header itself (a valid call with an Origin header gets 200 and no Access-Control-*)")
    void customers_sets_no_cors_headers() {
        Http.Response response = Http.get(customers(API), "Origin", ORIGIN, "Authorization", bearer(chain.token("sales")));

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.headerNames()).noneMatch(name -> name.startsWith("access-control-"));
    }

    // --- AC-8: correlation id ---

    @Test
    @DisplayName("AC-8: a new id is on the answer, and the same id is in the log of the gateway and in the log of customers")
    void created_id_is_in_both_logs() throws Exception {
        Http.Response response = Http.get(gateway(API), "Authorization", bearer(chain.token("sales")));

        String id = response.header(CORRELATION);
        assertThat(response.status()).isEqualTo(200);
        assertThat(id).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        awaitLogLine(chain.gatewayLog(), "[" + id + "]", "GET /api/v1/customers -> 200");
        awaitLogLine(chain.customersLog(), "[" + id + "]", "/api/v1/customers");
    }

    @Test
    @DisplayName("AC-8: an id sent by the caller is kept, and it is in both logs")
    void sent_id_is_in_both_logs() throws Exception {
        String id = "chain-" + UUID.randomUUID();

        Http.Response response = Http.get(gateway(API), CORRELATION, id, "Authorization", bearer(chain.token("sales")));

        assertThat(response.header(CORRELATION)).isEqualTo(id);
        awaitLogLine(chain.gatewayLog(), "[" + id + "]", "GET /api/v1/customers -> 200");
        awaitLogLine(chain.customersLog(), "[" + id + "]", "/api/v1/customers");
    }

    // --- AC-16: OpenAPI and Swagger UI through the gateway (dev) ---

    @Test
    @DisplayName("AC-16: /v3/api-docs is open, lists /api/v1/customers, and names the gateway as the server")
    void api_docs_through_the_gateway() {
        Http.Response response = Http.get(gateway("/v3/api-docs"));

        assertThat(response.status()).isEqualTo(200);
        assertThat(JsonPath.<Map<String, Object>>read(response.body(), "$.paths")).containsKey(API);
        // Proves the forwarded headers: the gateway sends its own address, and customers (native strategy in dev) uses it.
        assertThat(JsonPath.<String>read(response.body(), "$.servers[0].url")).isEqualTo(chain.gatewayUrl());
    }

    @Test
    @DisplayName("AC-16: Swagger UI and its configuration load through the gateway without a token")
    void swagger_ui_through_the_gateway() {
        Http.Response redirect = Http.get(gateway("/swagger-ui.html"));
        assertThat(redirect.status()).isEqualTo(302);
        assertThat(redirect.header("Location")).isEqualTo("/swagger-ui/index.html");

        Http.Response page = Http.get(gateway("/swagger-ui/index.html"));
        assertThat(page.status()).isEqualTo(200);
        assertThat(page.contentType()).startsWith("text/html");

        Http.Response config = Http.get(gateway("/v3/api-docs/swagger-config"));
        assertThat(config.status()).isEqualTo(200);
        assertThat(JsonPath.<String>read(config.body(), "$.url")).isEqualTo("/v3/api-docs");
    }

    @Test
    @DisplayName("AC-16 / AC-4: in dev, every other path still needs a token")
    void other_paths_still_need_a_token() {
        for (String path : List.of(API, "/v3/other", "/swagger-ui-other", "/actuator/info", "/anything")) {
            assertThat(Http.get(gateway(path)).status()).as("GET %s without a token", path).isEqualTo(401);
        }
    }

    // --- helpers ---

    /** Waits (with a deadline, no fixed sleep) until one line of the log has all the texts. Logs are written by another process. */
    private static void awaitLogLine(Path log, String... texts) throws IOException, InterruptedException {
        Instant deadline = Instant.now().plus(LOG_DEADLINE);
        while (true) {
            if (Files.readAllLines(log).stream().anyMatch(line -> List.of(texts).stream().allMatch(line::contains))) {
                return;
            }
            if (Instant.now().isAfter(deadline)) {
                throw new AssertionError("TIMEOUT: no line with " + List.of(texts) + " in " + log + " after " + LOG_DEADLINE.toSeconds() + " s");
            }
            Thread.sleep(200);
        }
    }
}
