package org.j2ra.minierp.customers;

import org.j2ra.minierp.customers.support.AbstractPostgresIntegrationTest;
import org.j2ra.minierp.customers.support.TestTokens;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Spec 0.3 scope 6: X-Correlation-Id is read and put in the log context. */
@ExtendWith(OutputCaptureExtension.class)
class CorrelationIdTest extends AbstractPostgresIntegrationTest {

    private static final String HEADER = "X-Correlation-Id";

    private MvcResult call(String correlationId) throws Exception {
        var request = get("/test/correlation")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestTokens.withRoles("SALES"));
        if (correlationId != null) {
            request.header(HEADER, correlationId);
        }
        return mockMvc.perform(request).andExpect(status().isOk()).andReturn();
    }

    @Test
    @DisplayName("Spec 0.3 scope 6: the id from the header is in the log context, in the log line and in the response")
    void header_id_is_used(CapturedOutput output) throws Exception {
        MvcResult result = call("test-id-123");
        assertThat(result.getResponse().getContentAsString()).isEqualTo("test-id-123");
        assertThat(result.getResponse().getHeader(HEADER)).isEqualTo("test-id-123");
        assertThat(output.getOut()).containsPattern("\\[test-id-123\\].*correlation test endpoint called");
    }

    @Test
    @DisplayName("Spec 0.3 scope 6: a call without the header gets a new id")
    void missing_id_is_created() throws Exception {
        MvcResult result = call(null);
        String id = result.getResponse().getHeader(HEADER);
        assertThat(id).matches("[0-9a-f-]{36}");
        assertThat(result.getResponse().getContentAsString()).isEqualTo(id);
    }

    @Test
    @DisplayName("Spec 0.3 scope 6: an id with unsafe characters is replaced (no fake log lines)")
    void unsafe_id_is_replaced() throws Exception {
        MvcResult result = call("bad id\nINFO fake line");
        assertThat(result.getResponse().getHeader(HEADER)).matches("[0-9a-f-]{36}");
    }

    @Test
    @DisplayName("Spec 0.3 scope 6: the id is removed from the log context after the request")
    void id_is_cleared_after_request() throws Exception {
        call("cleanup-check");
        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    @DisplayName("Spec 0.3 scope 6: a 401 answer also has the id (the filter runs before security)")
    void id_is_present_on_401() throws Exception {
        MvcResult result = mockMvc.perform(get("/test/secured").header(HEADER, "id-on-401"))
                .andExpect(status().isUnauthorized()).andReturn();
        assertThat(result.getResponse().getHeader(HEADER)).isEqualTo("id-on-401");
    }
}
