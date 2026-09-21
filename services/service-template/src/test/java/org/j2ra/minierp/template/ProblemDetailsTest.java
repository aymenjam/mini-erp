package org.j2ra.minierp.template;

import org.j2ra.minierp.template.support.AbstractPostgresIntegrationTest;
import org.j2ra.minierp.template.support.TestTokens;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** AC-11: every error is application/problem+json and has no stack trace. */
class ProblemDetailsTest extends AbstractPostgresIntegrationTest {

    private static final String SALES_TOKEN_HEADER_VALUE = "Bearer ";

    private static void assertProblem(ResultActions result, int status) throws Exception {
        result.andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.title").isNotEmpty())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.stackTrace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
        String body = result.andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("Exception").doesNotContain("\tat ").doesNotContain("org.springframework");
    }

    @Test
    @DisplayName("AC-11: an unexpected error gives 500 problem+json, without the internal message or a stack trace")
    void unexpected_error_is_problem_json_without_stack_trace() throws Exception {
        ResultActions result = mockMvc.perform(get("/test/boom")
                .header(HttpHeaders.AUTHORIZATION, SALES_TOKEN_HEADER_VALUE + TestTokens.withRoles("SALES")));
        assertProblem(result, 500);
        assertThat(result.andReturn().getResponse().getContentAsString()).doesNotContain("secret internal detail");
    }

    @Test
    @DisplayName("AC-11: an unknown path gives 404 problem+json")
    void unknown_path_is_problem_json() throws Exception {
        assertProblem(mockMvc.perform(get("/does-not-exist")
                .header(HttpHeaders.AUTHORIZATION, SALES_TOKEN_HEADER_VALUE + TestTokens.withRoles("SALES"))), 404);
    }

    @Test
    @DisplayName("AC-11: 401 (security filter) is problem+json")
    void unauthorized_is_problem_json() throws Exception {
        assertProblem(mockMvc.perform(get("/test/secured")), 401);
    }

    @Test
    @DisplayName("AC-11: 403 (security filter) is problem+json")
    void forbidden_is_problem_json() throws Exception {
        assertProblem(mockMvc.perform(get("/test/secured")
                .header(HttpHeaders.AUTHORIZATION, SALES_TOKEN_HEADER_VALUE + TestTokens.withRoles("WAREHOUSE"))), 403);
    }
}
