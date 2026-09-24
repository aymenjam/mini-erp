package org.j2ra.minierp.customers;

import org.j2ra.minierp.customers.support.AbstractPostgresIntegrationTest;
import org.j2ra.minierp.customers.support.CustomerRequests;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.j2ra.minierp.customers.support.CustomerRequests.URL;
import static org.j2ra.minierp.customers.support.CustomerRequests.bearer;
import static org.j2ra.minierp.customers.support.CustomerRequests.contact;
import static org.j2ra.minierp.customers.support.CustomerRequests.createCompany;
import static org.j2ra.minierp.customers.support.CustomerRequests.json;
import static org.j2ra.minierp.customers.support.CustomerRequests.read;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Spec 1.1, slice 2, AC-7 (and the contacts part of AC-3): contacts of a customer, real application and real PostgreSQL. */
class CustomerContactsApiTest extends AbstractPostgresIntegrationTest {

    private static final String UNKNOWN = "00000000-0000-4000-8000-000000000000";

    private ResultActions problem(ResultActions result, int status) throws Exception {
        return result.andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(status));
    }

    private String addContact(String customerId, String body) throws Exception {
        return mockMvc.perform(json(post(URL + "/" + customerId + "/contacts"), body, "SALES"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
    }

    private String getCustomer(String id) throws Exception {
        return mockMvc.perform(get(URL + "/" + id).header(HttpHeaders.AUTHORIZATION, bearer("SALES")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    @Test
    @DisplayName("AC-3: a new customer has empty lists of contacts and addresses")
    void new_customer_has_empty_lists() throws Exception {
        String id = createCompany(mockMvc, "ACME Ltd", null);

        mockMvc.perform(get(URL + "/" + id).header(HttpHeaders.AUTHORIZATION, bearer("SALES")))
                .andExpect(jsonPath("$.contacts").isArray())
                .andExpect(jsonPath("$.contacts.length()").value(0))
                .andExpect(jsonPath("$.addresses").isArray())
                .andExpect(jsonPath("$.addresses.length()").value(0));
    }

    @Test
    @DisplayName("AC-7: a contact is added: 201, Location, the whole customer with the contact and version 1, and GET shows it")
    void add_contact() throws Exception {
        String id = createCompany(mockMvc, "ACME Ltd", "FR1");

        ResultActions result = mockMvc.perform(json(post(URL + "/" + id + "/contacts"), contact("Jane", "Doe", false), "SALES"))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.contacts.length()").value(1))
                .andExpect(jsonPath("$.contacts[0].firstName").value("Jane"))
                .andExpect(jsonPath("$.contacts[0].lastName").value("Doe"))
                .andExpect(jsonPath("$.contacts[0].email").value("jane@acme.example"))
                .andExpect(jsonPath("$.contacts[0].jobTitle").value("Manager"))
                .andExpect(jsonPath("$.contacts[0].primary").value(false));
        String body = result.andReturn().getResponse().getContentAsString();
        String contactId = read(body, "$.contacts[0].id");
        assertThat(result.andReturn().getResponse().getHeader(HttpHeaders.LOCATION))
                .isEqualTo(URL + "/" + id + "/contacts/" + contactId);

        assertThat((String) read(getCustomer(id), "$.contacts[0].id")).isEqualTo(contactId);
    }

    @Test
    @DisplayName("AC-7: every change of a contact adds exactly 1 to the customer version (add, replace, remove)")
    void each_change_adds_one_to_the_version() throws Exception {
        String id = createCompany(mockMvc, "ACME Ltd", null);

        String added = addContact(id, contact("Jane", "Doe", false));
        assertThat((Integer) read(added, "$.version")).isEqualTo(1);
        String contactId = read(added, "$.contacts[0].id");

        mockMvc.perform(json(put(URL + "/" + id + "/contacts/" + contactId), contact("Janet", "Doe", false), "SALES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.contacts[0].firstName").value("Janet"));
        mockMvc.perform(delete(URL + "/" + id + "/contacts/" + contactId).header(HttpHeaders.AUTHORIZATION, bearer("SALES")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(3))
                .andExpect(jsonPath("$.contacts.length()").value(0));
        assertThat((Integer) read(getCustomer(id), "$.version")).isEqualTo(3);
    }

    @Test
    @DisplayName("AC-7: a second primary contact replaces the first one (no error), on add and on replace")
    void second_primary_replaces_the_first() throws Exception {
        String id = createCompany(mockMvc, "ACME Ltd", null);
        String first = addContact(id, contact("Jane", "Doe", true));
        String firstId = read(first, "$.contacts[0].id");

        String afterAdd = addContact(id, contact("John", "Smith", true));
        assertThat(CustomerRequests.<List<String>>read(afterAdd, "$.contacts[?(@.primary == true)].firstName")).containsExactly("John");
        assertThat(CustomerRequests.<List<String>>read(afterAdd, "$.contacts[?(@.primary == false)].firstName")).containsExactly("Jane");

        String afterReplace = mockMvc.perform(json(put(URL + "/" + id + "/contacts/" + firstId), contact("Jane", "Doe", true), "SALES"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(CustomerRequests.<List<String>>read(afterReplace, "$.contacts[?(@.primary == true)].firstName")).containsExactly("Jane");
        assertThat(CustomerRequests.<List<String>>read(getCustomer(id), "$.contacts[?(@.primary == true)].firstName")).containsExactly("Jane");
    }

    @Test
    @DisplayName("AC-7: contacts are listed by last name, then first name")
    void contacts_are_ordered() throws Exception {
        String id = createCompany(mockMvc, "ACME Ltd", null);
        addContact(id, contact("Zoe", "Brown", false));
        addContact(id, contact("Max", "Adams", false));

        mockMvc.perform(get(URL + "/" + id).header(HttpHeaders.AUTHORIZATION, bearer("SALES")))
                .andExpect(jsonPath("$.contacts[0].lastName").value("Adams"))
                .andExpect(jsonPath("$.contacts[1].lastName").value("Brown"));
    }

    @Test
    @DisplayName("AC-7: bad contact fields give 400 with the field names and store nothing")
    void invalid_contact_is_400() throws Exception {
        String id = createCompany(mockMvc, "ACME Ltd", null);

        problem(mockMvc.perform(json(post(URL + "/" + id + "/contacts"),
                "{\"firstName\":\" \",\"email\":\"nope\",\"phone\":\"%s\"}".formatted("1".repeat(51)), "SALES")), 400)
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("firstName")))
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("lastName")))
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("email")))
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("phone")))
                .andExpect(content().string(not(containsString("Exception"))));
        assertThat((Integer) read(getCustomer(id), "$.version")).isZero();
    }

    @Test
    @DisplayName("AC-7: unknown customer or unknown contact gives 404; a bad id gives 400")
    void unknown_ids() throws Exception {
        String id = createCompany(mockMvc, "ACME Ltd", null);

        problem(mockMvc.perform(json(post(URL + "/" + UNKNOWN + "/contacts"), contact("Jane", "Doe", false), "SALES")), 404);
        problem(mockMvc.perform(json(put(URL + "/" + id + "/contacts/" + UNKNOWN), contact("Jane", "Doe", false), "SALES")), 404);
        problem(mockMvc.perform(delete(URL + "/" + id + "/contacts/" + UNKNOWN).header(HttpHeaders.AUTHORIZATION, bearer("SALES"))), 404);
        problem(mockMvc.perform(delete(URL + "/" + UNKNOWN + "/contacts/" + UNKNOWN).header(HttpHeaders.AUTHORIZATION, bearer("SALES"))), 404);
        problem(mockMvc.perform(delete(URL + "/" + id + "/contacts/not-a-uuid").header(HttpHeaders.AUTHORIZATION, bearer("SALES"))), 400);
    }

    @Test
    @DisplayName("AC-7: an inactive customer refuses contact changes (409), and accepts them after reactivation")
    void inactive_customer_refuses_changes() throws Exception {
        String id = createCompany(mockMvc, "ACME Ltd", null);
        String contactId = read(addContact(id, contact("Jane", "Doe", false)), "$.contacts[0].id");
        mockMvc.perform(json(post(URL + "/" + id + "/deactivate"), "", "SALES")).andExpect(status().isOk());

        problem(mockMvc.perform(json(post(URL + "/" + id + "/contacts"), contact("John", "Smith", false), "SALES")), 409);
        problem(mockMvc.perform(json(put(URL + "/" + id + "/contacts/" + contactId), contact("Janet", "Doe", false), "SALES")), 409);
        problem(mockMvc.perform(delete(URL + "/" + id + "/contacts/" + contactId).header(HttpHeaders.AUTHORIZATION, bearer("SALES"))), 409);
        assertThat((Integer) read(getCustomer(id), "$.contacts.length()")).isEqualTo(1);

        mockMvc.perform(json(post(URL + "/" + id + "/reactivate"), "", "SALES")).andExpect(status().isOk());
        mockMvc.perform(json(post(URL + "/" + id + "/contacts"), contact("John", "Smith", false), "SALES"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("AC-7: a customer update keeps its contacts")
    void customer_update_keeps_contacts() throws Exception {
        String id = createCompany(mockMvc, "ACME Ltd", "FR1");
        addContact(id, contact("Jane", "Doe", true));

        mockMvc.perform(json(put(URL + "/" + id),
                        "{\"version\":1,\"type\":\"COMPANY\",\"name\":\"ACME Group\",\"vatNumber\":\"FR1\"}", "SALES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.contacts.length()").value(1));
    }

    @Test
    @DisplayName("AC-7: the database allows only one primary contact per customer (last guard behind the domain)")
    void database_guards_the_primary_contact() throws Exception {
        String id = createCompany(mockMvc, "ACME Ltd", null);
        String insert = """
                INSERT INTO customer_contact (id, version, customer_id, first_name, last_name, is_primary)
                VALUES (gen_random_uuid(), 0, '%s', 'A', 'B', %s)""";
        jdbc.update(insert.formatted(id, "true"));
        jdbc.update(insert.formatted(id, "false"));
        jdbc.update(insert.formatted(id, "false"));

        assertThatThrownBy(() -> jdbc.update(insert.formatted(id, "true"))).hasMessageContaining("ux_customer_contact_primary");
    }

    @Test
    @DisplayName("AC-7: the database deletes the contacts with their customer (safety net, the app never deletes customers)")
    void contacts_go_with_the_customer() throws Exception {
        String id = createCompany(mockMvc, "ACME Ltd", null);
        addContact(id, contact("Jane", "Doe", false));

        jdbc.update("DELETE FROM customer WHERE id = ?::uuid", id);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM customer_contact", Integer.class)).isZero();
    }
}
