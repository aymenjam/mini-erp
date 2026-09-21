package org.j2ra.minierp.customers;

import org.j2ra.minierp.customers.support.AbstractPostgresIntegrationTest;
import org.j2ra.minierp.customers.support.TestTokens;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** AC-22: without the profile dev, the documentation URLs are refused. Two locks, so each one is tested. */
class ApiDocsOffByDefaultTest extends AbstractPostgresIntegrationTest {

    private static final String[] URLS = {"/v3/api-docs", "/swagger-ui.html", "/swagger-ui/index.html"};

    @Test
    @DisplayName("AC-22: without a token the documentation URLs give 401 (security lock: no permitAll)")
    void without_token_is_401() throws Exception {
        for (String url : URLS) {
            mockMvc.perform(get(url)).andExpect(status().isUnauthorized());
        }
    }

    @Test
    @DisplayName("AC-22: even with a valid token the documentation URLs give 404 (springdoc lock: not switched on)")
    void with_token_is_404() throws Exception {
        String bearer = "Bearer " + TestTokens.withRoles("ADMIN");
        for (String url : URLS) {
            mockMvc.perform(get(url).header(HttpHeaders.AUTHORIZATION, bearer)).andExpect(status().isNotFound());
        }
    }
}
