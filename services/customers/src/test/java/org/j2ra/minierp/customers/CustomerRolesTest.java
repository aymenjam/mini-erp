package org.j2ra.minierp.customers;

import org.j2ra.minierp.customers.support.AbstractPostgresIntegrationTest;
import org.j2ra.minierp.customers.support.TestTokens;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/** AC-11: every customer endpoint checks the token and the role: 401 without token, 403 for other roles, allowed for SALES and ADMIN. */
class CustomerRolesTest extends AbstractPostgresIntegrationTest {

    private static final String ID = UUID.randomUUID().toString();
    private static final String BASE = "/api/v1/customers";
    private static final String NEW = "{\"type\":\"COMPANY\",\"name\":\"ACME\"}";
    private static final String CHANGE = "{\"version\":0,\"type\":\"COMPANY\",\"name\":\"ACME\"}";

    record Endpoint(HttpMethod method, String path, String body) {
        @Override
        public String toString() {
            return method + " " + path;
        }
    }

    static List<Endpoint> endpoints() {
        return List.of(
                new Endpoint(HttpMethod.GET, BASE, null),
                new Endpoint(HttpMethod.POST, BASE, NEW),
                new Endpoint(HttpMethod.GET, BASE + "/" + ID, null),
                new Endpoint(HttpMethod.PUT, BASE + "/" + ID, CHANGE),
                new Endpoint(HttpMethod.POST, BASE + "/" + ID + "/deactivate", null),
                new Endpoint(HttpMethod.POST, BASE + "/" + ID + "/reactivate", null));
    }

    private MvcResult call(Endpoint endpoint, String token) throws Exception {
        MockHttpServletRequestBuilder builder = request(endpoint.method(), endpoint.path());
        if (token != null) {
            builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        if (endpoint.body() != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(endpoint.body());
        }
        return mockMvc.perform(builder).andReturn();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("endpoints")
    @DisplayName("AC-11: no token gives 401")
    void no_token_is_401(Endpoint endpoint) throws Exception {
        assertThat(call(endpoint, null).getResponse().getStatus()).isEqualTo(401);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("endpoints")
    @DisplayName("AC-11: WAREHOUSE and a token without role get 403 problem+json")
    void other_roles_are_403(Endpoint endpoint) throws Exception {
        for (String token : List.of(TestTokens.withRoles("WAREHOUSE"), TestTokens.withRoles())) {
            MvcResult result = call(endpoint, token);
            assertThat(result.getResponse().getStatus()).isEqualTo(403);
            assertThat(result.getResponse().getContentType()).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("endpoints")
    @DisplayName("AC-11: SALES and ADMIN pass security (the answer is not 401 or 403)")
    void sales_and_admin_are_allowed(Endpoint endpoint) throws Exception {
        for (String role : List.of("SALES", "ADMIN")) {
            int status = call(endpoint, TestTokens.withRoles(role)).getResponse().getStatus();
            assertThat(status).isNotIn(401, 403);
        }
    }
}
