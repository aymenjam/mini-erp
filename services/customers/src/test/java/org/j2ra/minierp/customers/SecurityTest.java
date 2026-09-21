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

/** Resource server security on the test endpoint /test/secured (needs role SALES). */
class SecurityTest extends AbstractPostgresIntegrationTest {

    private static final String SECURED = "/test/secured";

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    @DisplayName("AC-9: no token gives 401")
    void no_token_is_401() throws Exception {
        mockMvc.perform(get(SECURED))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists(HttpHeaders.WWW_AUTHENTICATE));
    }

    @Test
    @DisplayName("AC-9: an expired token gives 401")
    void expired_token_is_401() throws Exception {
        mockMvc.perform(get(SECURED).header(HttpHeaders.AUTHORIZATION, bearer(TestTokens.expired("SALES"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("AC-9: a token signed with another key gives 401")
    void wrongly_signed_token_is_401() throws Exception {
        mockMvc.perform(get(SECURED).header(HttpHeaders.AUTHORIZATION, bearer(TestTokens.wrongSignature("SALES"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("AC-9: a token from another issuer gives 401")
    void wrong_issuer_token_is_401() throws Exception {
        mockMvc.perform(get(SECURED).header(HttpHeaders.AUTHORIZATION, bearer(TestTokens.wrongIssuer("SALES"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("AC-9: a valid token without the needed role gives 403")
    void token_without_role_is_403() throws Exception {
        mockMvc.perform(get(SECURED).header(HttpHeaders.AUTHORIZATION, bearer(TestTokens.withRoles("WAREHOUSE"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("AC-9: a valid token without any role gives 403")
    void token_with_no_roles_is_403() throws Exception {
        mockMvc.perform(get(SECURED).header(HttpHeaders.AUTHORIZATION, bearer(TestTokens.withRoles())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("AC-9 and AC-10: a valid token with role SALES gives 200, and the authority is ROLE_SALES")
    void token_with_role_is_200_and_role_is_mapped() throws Exception {
        mockMvc.perform(get(SECURED).header(HttpHeaders.AUTHORIZATION, bearer(TestTokens.withRoles("SALES", "ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0]").value("ROLE_ADMIN"))
                .andExpect(jsonPath("$[1]").value("ROLE_SALES"))
                .andExpect(jsonPath("$.length()").value(2));
    }
}
