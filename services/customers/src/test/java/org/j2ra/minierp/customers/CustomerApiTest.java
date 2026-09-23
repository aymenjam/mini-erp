package org.j2ra.minierp.customers;

import org.j2ra.minierp.customers.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.j2ra.minierp.customers.support.CustomerRequests.URL;
import static org.j2ra.minierp.customers.support.CustomerRequests.bearer;
import static org.j2ra.minierp.customers.support.CustomerRequests.company;
import static org.j2ra.minierp.customers.support.CustomerRequests.createCompany;
import static org.j2ra.minierp.customers.support.CustomerRequests.json;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Spec 1.1, slice 1: the customer API against the real application and a real PostgreSQL. */
class CustomerApiTest extends AbstractPostgresIntegrationTest {

    private ResultActions problem(ResultActions result, int status) throws Exception {
        return result.andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(status));
    }

    @Test
    @DisplayName("AC-1: a COMPANY is created: 201, Location, ACTIVE, version 0, VAT number in upper case")
    void create_company() throws Exception {
        mockMvc.perform(json(post(URL), company("ACME Ltd", " fr123 "), "SALES"))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, containsString(URL + "/")))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.type").value("COMPANY"))
                .andExpect(jsonPath("$.name").value("ACME Ltd"))
                .andExpect(jsonPath("$.vatNumber").value("FR123"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    @DisplayName("AC-1: an INDIVIDUAL is created without VAT number; the Location header leads to the customer")
    void create_individual_and_follow_location() throws Exception {
        String location = mockMvc.perform(json(post(URL), """
                        {"type":"INDIVIDUAL","name":"Jane Doe","email":"jane@example.org"}
                        """, "ADMIN"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("INDIVIDUAL"))
                .andExpect(jsonPath("$.vatNumber").doesNotExist())
                .andReturn().getResponse().getHeader(HttpHeaders.LOCATION);

        mockMvc.perform(get(location).header(HttpHeaders.AUTHORIZATION, bearer("SALES")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Jane Doe"));
    }

    @Test
    @DisplayName("AC-2: bad fields give 400 problem+json with the field names, no stack trace, and no customer is stored")
    void invalid_body_is_400_with_field_names() throws Exception {
        problem(mockMvc.perform(json(post(URL), """
                {"type":"COMPANY","name":"%s","email":"not-an-email"}
                """.formatted("x".repeat(201)), "SALES")), 400)
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("name")))
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("email")))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(content().string(not(containsString("Exception"))))
                .andExpect(content().string(not(containsString("not-an-email"))));

        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, bearer("SALES")))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("AC-2: a missing type, a missing name and a body that is not JSON give 400")
    void missing_fields_are_400() throws Exception {
        problem(mockMvc.perform(json(post(URL), "{}", "SALES")), 400)
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("type")))
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("name")));
        problem(mockMvc.perform(json(post(URL), "{\"type\":\"ROBOT\",\"name\":\"R2\"}", "SALES")), 400);
        problem(mockMvc.perform(json(post(URL), "not json", "SALES")), 400);
    }

    @Test
    @DisplayName("AC-2: a VAT number on an INDIVIDUAL gives 400 that names vatNumber")
    void vat_on_individual_is_400() throws Exception {
        problem(mockMvc.perform(json(post(URL), """
                {"type":"INDIVIDUAL","name":"Jane","vatNumber":"FR1"}
                """, "SALES")), 400)
                .andExpect(jsonPath("$.errors[0].field").value("vatNumber"));
    }

    @Test
    @DisplayName("AC-3: get by id returns the customer; an unknown id gives 404; a bad id gives 400")
    void get_and_not_found() throws Exception {
        String id = createCompany(mockMvc, "ACME Ltd", "FR1");

        mockMvc.perform(get(URL + "/" + id).header(HttpHeaders.AUTHORIZATION, bearer("SALES")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.email").value("contact@acme.example"));
        problem(mockMvc.perform(get(URL + "/00000000-0000-4000-8000-000000000000").header(HttpHeaders.AUTHORIZATION, bearer("SALES"))), 404);
        problem(mockMvc.perform(get(URL + "/not-a-uuid").header(HttpHeaders.AUTHORIZATION, bearer("SALES"))), 400);
    }

    @Test
    @DisplayName("AC-4: update with the right version works and adds 1; the same old version then gives 409")
    void update_and_optimistic_locking() throws Exception {
        String id = createCompany(mockMvc, "ACME Ltd", "FR1");
        String body = """
                {"version":0,"type":"COMPANY","name":"ACME Group","vatNumber":"FR1"}
                """;

        mockMvc.perform(json(put(URL + "/" + id), body, "SALES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("ACME Group"))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.version").value(1));
        problem(mockMvc.perform(json(put(URL + "/" + id), body, "SALES")), 409);
        mockMvc.perform(get(URL + "/" + id).header(HttpHeaders.AUTHORIZATION, bearer("SALES")))
                .andExpect(jsonPath("$.name").value("ACME Group"))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    @DisplayName("AC-4: update without a version gives 400; update of an unknown customer gives 404")
    void update_needs_version_and_existing_customer() throws Exception {
        String id = createCompany(mockMvc, "ACME Ltd", null);

        problem(mockMvc.perform(json(put(URL + "/" + id), "{\"type\":\"COMPANY\",\"name\":\"X\"}", "SALES")), 400)
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("version")));
        problem(mockMvc.perform(json(put(URL + "/00000000-0000-4000-8000-000000000000"),
                "{\"version\":0,\"type\":\"COMPANY\",\"name\":\"X\"}", "SALES")), 404);
    }

    @Test
    @DisplayName("AC-5: a second customer with the same VAT number gives 409, even with other spaces and letter case")
    void duplicate_vat_number_is_409() throws Exception {
        createCompany(mockMvc, "ACME Ltd", "FR123");

        problem(mockMvc.perform(json(post(URL), company("Other Ltd", " fr123"), "SALES")), 409);
        // A customer without VAT number is never a duplicate.
        mockMvc.perform(json(post(URL), company("No VAT 1", null), "SALES")).andExpect(status().isCreated());
        mockMvc.perform(json(post(URL), company("No VAT 2", null), "SALES")).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("AC-5: an update to the VAT number of another customer gives 409")
    void update_to_duplicate_vat_number_is_409() throws Exception {
        createCompany(mockMvc, "First", "FR1");
        String second = createCompany(mockMvc, "Second", "FR2");

        problem(mockMvc.perform(json(put(URL + "/" + second),
                "{\"version\":0,\"type\":\"COMPANY\",\"name\":\"Second\",\"vatNumber\":\"FR1\"}", "SALES")), 409);
    }

    @Test
    @DisplayName("AC-6: deactivate, then update gives 409, then reactivate allows the update again")
    void deactivate_and_reactivate() throws Exception {
        String id = createCompany(mockMvc, "ACME Ltd", "FR1");
        String update = "{\"version\":%d,\"type\":\"COMPANY\",\"name\":\"ACME Group\",\"vatNumber\":\"FR1\"}";

        mockMvc.perform(json(post(URL + "/" + id + "/deactivate"), "", "SALES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"))
                .andExpect(jsonPath("$.version").value(1));
        problem(mockMvc.perform(json(put(URL + "/" + id), update.formatted(1), "SALES")), 409);

        mockMvc.perform(json(post(URL + "/" + id + "/reactivate"), "", "SALES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.version").value(2));
        mockMvc.perform(json(put(URL + "/" + id), update.formatted(2), "SALES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("ACME Group"));
    }

    @Test
    @DisplayName("AC-6: deactivating twice changes nothing the second time; an unknown customer gives 404")
    void deactivate_twice_and_unknown() throws Exception {
        String id = createCompany(mockMvc, "ACME Ltd", null);

        mockMvc.perform(json(post(URL + "/" + id + "/deactivate"), "", "SALES")).andExpect(jsonPath("$.version").value(1));
        mockMvc.perform(json(post(URL + "/" + id + "/deactivate"), "", "SALES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1));
        problem(mockMvc.perform(json(post(URL + "/00000000-0000-4000-8000-000000000000/deactivate"), "", "SALES")), 404);
        problem(mockMvc.perform(json(post(URL + "/00000000-0000-4000-8000-000000000000/reactivate"), "", "SALES")), 404);
    }

    @Test
    @DisplayName("AC-6: a customer cannot be deleted: there is no DELETE on it")
    void no_delete() throws Exception {
        String id = createCompany(mockMvc, "ACME Ltd", null);

        mockMvc.perform(delete(URL + "/" + id).header(HttpHeaders.AUTHORIZATION, bearer("ADMIN")))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(get(URL + "/" + id).header(HttpHeaders.AUTHORIZATION, bearer("ADMIN"))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Scope 4: the list shows the stored customers as summaries, sorted by id")
    void list_shows_customers() throws Exception {
        String id = createCompany(mockMvc, "ACME Ltd", "FR1");

        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, bearer("SALES")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(id))
                .andExpect(jsonPath("$.content[0].name").value("ACME Ltd"))
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.content[0].vatNumber").doesNotExist());
    }

    @Test
    @DisplayName("AC-14: the database itself refuses a VAT number on an INDIVIDUAL (last guard behind the domain)")
    void database_guards_the_vat_rule() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO customer (id, version, type, name, vat_number, status, created_at, updated_at)
                VALUES (gen_random_uuid(), 0, 'INDIVIDUAL', 'Jane', 'FR1', 'ACTIVE', now(), now())"""))
                .hasMessageContaining("ck_customer_vat_only_company");
    }
}
