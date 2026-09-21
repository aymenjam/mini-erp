package org.j2ra.minierp.customers;

import org.j2ra.minierp.customers.support.AbstractPostgresIntegrationTest;
import org.j2ra.minierp.customers.support.TestTokens;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** AC-14: page, size and sort are validated. A bad value gives 400 problem+json, never 500. */
class CustomerListValidationTest extends AbstractPostgresIntegrationTest {

    private ResultActions list(String query) throws Exception {
        return mockMvc.perform(get("/api/v1/customers" + query)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestTokens.withRoles("SALES")));
    }

    @ParameterizedTest(name = "AC-14: {0} gives 400 problem+json")
    @ValueSource(strings = {
            "?page=-1", "?page=abc", "?size=0", "?size=-5", "?size=101", "?size=abc",
            "?sort=name,asc", "?sort=id,up", "?sort=id", "?sort=id,asc,x", "?sort=,asc", "?sort=ID,asc"})
    void invalid_value_is_400(String query) throws Exception {
        ResultActions result = list(query);
        result.andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.trace").doesNotExist());
        assertThat(result.andReturn().getResponse().getContentAsString())
                .doesNotContain("Exception").doesNotContain("\tat ").doesNotContain("org.springframework");
    }

    @Test
    @DisplayName("AC-14: without parameters the defaults are page 0 and size 20, in the pagination shape of constitution.md")
    void defaults_and_shape() throws Exception {
        list("").andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
    }

    @Test
    @DisplayName("AC-14: an empty value (?page=&size=&sort=) counts as absent and gets the default, as everywhere in Spring MVC")
    void empty_value_uses_the_default() throws Exception {
        list("?page=&size=&sort=").andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    @DisplayName("AC-14: size 100 (the maximum) is accepted")
    void size_100_is_accepted() throws Exception {
        list("?size=100").andExpect(status().isOk()).andExpect(jsonPath("$.size").value(100));
    }

    @Test
    @DisplayName("AC-14: sort=id,desc is accepted, and so is sort=id,asc")
    void sort_by_id_is_accepted() throws Exception {
        list("?sort=id,desc").andExpect(status().isOk());
        list("?sort=id,asc").andExpect(status().isOk());
    }

    @Test
    @DisplayName("AC-14: a page beyond the data is accepted and echoed")
    void page_is_echoed() throws Exception {
        list("?page=3&size=5").andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(3))
                .andExpect(jsonPath("$.size").value(5));
    }
}
