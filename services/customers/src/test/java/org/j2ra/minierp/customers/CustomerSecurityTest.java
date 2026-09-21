package org.j2ra.minierp.customers;

import org.j2ra.minierp.customers.support.AbstractPostgresIntegrationTest;
import org.j2ra.minierp.customers.support.TestTokens;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AC-4: the service checks the token itself (it does not trust the gateway alone) and the role of the endpoint.
 * The tokens are really signed, so signature, expiry and issuer are checked as in production.
 */
class CustomerSecurityTest extends AbstractPostgresIntegrationTest {

    private static final String URL = "/api/v1/customers";

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    @DisplayName("AC-4: no token gives 401 problem+json")
    void no_token_is_401() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists(HttpHeaders.WWW_AUTHENTICATE))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    @DisplayName("AC-4: an expired token gives 401")
    void expired_token_is_401() throws Exception {
        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, bearer(TestTokens.expired("SALES"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("AC-4: a token signed with another key gives 401")
    void wrongly_signed_token_is_401() throws Exception {
        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, bearer(TestTokens.wrongSignature("SALES"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("AC-4: a token from another issuer gives 401")
    void wrong_issuer_token_is_401() throws Exception {
        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, bearer(TestTokens.wrongIssuer("SALES"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Spec 0.4 scope 1: role WAREHOUSE gets 403 problem+json")
    void warehouse_is_403() throws Exception {
        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, bearer(TestTokens.withRoles("WAREHOUSE"))))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("Spec 0.4 scope 1: a valid token without any role gets 403")
    void no_role_is_403() throws Exception {
        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, bearer(TestTokens.withRoles())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Spec 0.4 scope 1: SALES gets 200 and an empty page")
    void sales_gets_empty_page() throws Exception {
        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, bearer(TestTokens.withRoles("SALES"))))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    @DisplayName("Spec 0.4 scope 1: ADMIN gets 200")
    void admin_is_200() throws Exception {
        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, bearer(TestTokens.withRoles("ADMIN"))))
                .andExpect(status().isOk());
    }
}
