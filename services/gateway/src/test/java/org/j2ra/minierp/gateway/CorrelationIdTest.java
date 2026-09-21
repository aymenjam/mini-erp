package org.j2ra.minierp.gateway;

import org.j2ra.minierp.gateway.support.AbstractGatewayTest;
import org.j2ra.minierp.gateway.support.ContextProbeConfig;
import org.j2ra.minierp.gateway.support.TestTokens;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/** AC-8: X-Correlation-Id is created or kept, sent to the service, on every answer, and in the log line of the gateway. */
@ExtendWith(OutputCaptureExtension.class)
@Import(ContextProbeConfig.class)
class CorrelationIdTest extends AbstractGatewayTest {

    private static final String HEADER = "X-Correlation-Id";

    private WebTestClient.RequestHeadersSpec<?> apiCall() {
        return client.get().uri("/api/v1/customers").header(HttpHeaders.AUTHORIZATION, bearer(TestTokens.valid("SALES")));
    }

    private static String idOf(EntityExchangeResult<?> result) {
        assertThat(result.getResponseHeaders().get(HEADER)).as("exactly one X-Correlation-Id value").hasSize(1);
        return result.getResponseHeaders().getFirst(HEADER);
    }

    @Test
    @DisplayName("AC-8: a call without the header gets a new UUID, and the service gets the same one")
    void missing_id_is_created() {
        EntityExchangeResult<byte[]> result = apiCall().exchange().expectStatus().isOk().expectBody().returnResult();

        String id = idOf(result);
        assertThat(id).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        assertThat(downstream.lastRequest().header(HEADER)).isEqualTo(id);
    }

    @Test
    @DisplayName("AC-8: a safe id from the caller is kept, on the answer and for the service")
    void safe_id_is_kept() {
        EntityExchangeResult<byte[]> result = apiCall().header(HEADER, "front-end-42").exchange().expectStatus().isOk().expectBody().returnResult();

        assertThat(idOf(result)).isEqualTo("front-end-42");
        assertThat(downstream.lastRequest().header(HEADER)).isEqualTo("front-end-42");
    }

    @Test
    @DisplayName("AC-8: an unsafe id (spaces, symbols, too long) is replaced. The service never gets it")
    void unsafe_id_is_replaced() {
        for (String unsafe : new String[]{"bad id with spaces", "a;b<script>", "x".repeat(65), "id|INFO fake line"}) {
            downstream.reset();
            EntityExchangeResult<byte[]> result = apiCall().header(HEADER, unsafe).exchange().expectStatus().isOk().expectBody().returnResult();

            assertThat(idOf(result)).as("answer for '%s'", unsafe).matches("[0-9a-f-]{36}");
            assertThat(downstream.lastRequest().header(HEADER)).as("service for '%s'", unsafe).isEqualTo(idOf(result));
        }
    }

    @Test
    @DisplayName("AC-8: the answer has one id even if the service sends its own header")
    void one_value_when_the_service_also_sends_the_header() {
        downstream.respondWithHeader(HEADER, "from-the-service");

        EntityExchangeResult<byte[]> result = apiCall().header(HEADER, "from-the-gateway").exchange().expectBody().returnResult();

        assertThat(idOf(result)).isEqualTo("from-the-gateway");
    }

    @Test
    @DisplayName("AC-8 / AC-21: the id is on the errors made by the gateway (401 and 404)")
    void id_is_on_gateway_errors() {
        EntityExchangeResult<byte[]> unauthorized = client.get().uri("/api/v1/customers").header(HEADER, "id-on-401")
                .exchange().expectStatus().isUnauthorized().expectBody().returnResult();
        assertThat(idOf(unauthorized)).isEqualTo("id-on-401");

        EntityExchangeResult<byte[]> notFound = client.get().uri("/nothing").header(HEADER, "id-on-404")
                .header(HttpHeaders.AUTHORIZATION, bearer(TestTokens.valid("SALES")))
                .exchange().expectStatus().isNotFound().expectBody().returnResult();
        assertThat(idOf(notFound)).isEqualTo("id-on-404");
    }

    @Test
    @DisplayName("AC-8: one INFO line per request has the id, the method, the path and the status")
    void log_line_has_the_id(CapturedOutput output) {
        apiCall().header(HEADER, "log-check-1").exchange().expectStatus().isOk();

        assertThat(output.getOut()).containsPattern("INFO \\[log-check-1\\].*GET /api/v1/customers -> 200 OK");
    }

    @Test
    @DisplayName("AC-8: the log line of a 401 has the id too, and it does not show the query string")
    void log_line_of_401_has_the_id(CapturedOutput output) {
        client.get().uri("/api/v1/customers?email=private@example.test").header(HEADER, "log-check-2").exchange().expectStatus().isUnauthorized();

        assertThat(output.getOut()).containsPattern("INFO \\[log-check-2\\].*GET /api/v1/customers -> 401 UNAUTHORIZED");
        assertThat(output.getOut()).doesNotContain("private@example.test");
    }

    @Test
    @DisplayName("AC-8: the id is in the Reactor context for code deeper in the chain")
    void id_is_in_the_reactor_context() {
        EntityExchangeResult<byte[]> result = apiCall().header(HEADER, "context-check").exchange().expectStatus().isOk().expectBody().returnResult();

        assertThat(idOf(result)).isEqualTo("context-check");
        assertThat(ContextProbeConfig.SEEN.get()).isEqualTo("context-check");
    }

    @Test
    @DisplayName("AC-8: the id is not left in the MDC of the thread after a request")
    void id_is_not_left_in_the_mdc() {
        apiCall().header(HEADER, "mdc-check").exchange().expectStatus().isOk();

        assertThat(MDC.get("correlationId")).isNull();
    }
}
