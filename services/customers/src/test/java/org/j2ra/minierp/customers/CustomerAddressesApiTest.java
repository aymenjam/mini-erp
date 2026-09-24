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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.j2ra.minierp.customers.support.CustomerRequests.URL;
import static org.j2ra.minierp.customers.support.CustomerRequests.address;
import static org.j2ra.minierp.customers.support.CustomerRequests.bearer;
import static org.j2ra.minierp.customers.support.CustomerRequests.createCompany;
import static org.j2ra.minierp.customers.support.CustomerRequests.json;
import static org.j2ra.minierp.customers.support.CustomerRequests.read;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Spec 1.1, slice 2, AC-8 (and the addresses part of AC-3): addresses of a customer, real application and real PostgreSQL. */
class CustomerAddressesApiTest extends AbstractPostgresIntegrationTest {

    private static final String UNKNOWN = "00000000-0000-4000-8000-000000000000";

    private ResultActions problem(ResultActions result, int status) throws Exception {
        return result.andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(status));
    }

    private String addAddress(String customerId, String body) throws Exception {
        return mockMvc.perform(json(post(URL + "/" + customerId + "/addresses"), body, "SALES"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
    }

    private String getCustomer(String id) throws Exception {
        return mockMvc.perform(get(URL + "/" + id).header(HttpHeaders.AUTHORIZATION, bearer("SALES")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    @Test
    @DisplayName("AC-8: an address is added: 201, Location, the whole customer with the address and version 1, and GET shows it")
    void add_address() throws Exception {
        String id = createCompany(mockMvc, "ACME Ltd", "FR1");

        var result = mockMvc.perform(json(post(URL + "/" + id + "/addresses"), address("BILLING", "Paris", "FR", true), "SALES"))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.addresses.length()").value(1))
                .andExpect(jsonPath("$.addresses[0].type").value("BILLING"))
                .andExpect(jsonPath("$.addresses[0].line1").value("1 rue de la Paix"))
                .andExpect(jsonPath("$.addresses[0].postalCode").value("75002"))
                .andExpect(jsonPath("$.addresses[0].city").value("Paris"))
                .andExpect(jsonPath("$.addresses[0].country").value("FR"))
                .andExpect(jsonPath("$.addresses[0].defaultForType").value(true))
                .andReturn().getResponse();
        String addressId = read(result.getContentAsString(), "$.addresses[0].id");
        assertThat(result.getHeader(HttpHeaders.LOCATION)).isEqualTo(URL + "/" + id + "/addresses/" + addressId);

        assertThat((String) read(getCustomer(id), "$.addresses[0].id")).isEqualTo(addressId);
    }

    @Test
    @DisplayName("AC-8: every change of an address adds exactly 1 to the customer version (add, replace, remove)")
    void each_change_adds_one_to_the_version() throws Exception {
        String id = createCompany(mockMvc, "ACME Ltd", null);

        String added = addAddress(id, address("BILLING", "Paris", "FR", false));
        assertThat((Integer) read(added, "$.version")).isEqualTo(1);
        String addressId = read(added, "$.addresses[0].id");

        mockMvc.perform(json(put(URL + "/" + id + "/addresses/" + addressId), address("SHIPPING", "Lyon", "FR", false), "SALES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.addresses[0].type").value("SHIPPING"))
                .andExpect(jsonPath("$.addresses[0].city").value("Lyon"));
        mockMvc.perform(delete(URL + "/" + id + "/addresses/" + addressId).header(HttpHeaders.AUTHORIZATION, bearer("SALES")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(3))
                .andExpect(jsonPath("$.addresses.length()").value(0));
        assertThat((Integer) read(getCustomer(id), "$.version")).isEqualTo(3);
    }

    @Test
    @DisplayName("AC-8: a new default replaces the old default of the same type only; BILLING and SHIPPING are independent")
    void default_is_per_type() throws Exception {
        String id = createCompany(mockMvc, "ACME Ltd", null);
        String billing1 = addAddress(id, address("BILLING", "Paris", "FR", true));
        String billing1Id = read(billing1, "$.addresses[0].id");
        addAddress(id, address("SHIPPING", "Lyon", "FR", true));

        String afterAdd = addAddress(id, address("BILLING", "Nice", "FR", true));
        assertThat(CustomerRequests.<List<String>>read(afterAdd, "$.addresses[?(@.defaultForType == true)].city"))
                .containsExactlyInAnyOrder("Nice", "Lyon");

        String afterReplace = mockMvc.perform(json(put(URL + "/" + id + "/addresses/" + billing1Id),
                        address("BILLING", "Paris", "FR", true), "SALES"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(CustomerRequests.<List<String>>read(afterReplace, "$.addresses[?(@.defaultForType == true)].city"))
                .containsExactlyInAnyOrder("Paris", "Lyon");
        assertThat(CustomerRequests.<List<String>>read(getCustomer(id), "$.addresses[?(@.defaultForType == false)].city"))
                .containsExactly("Nice");
    }

    @Test
    @DisplayName("AC-2, AC-8: the country is an ISO code: 'fr' becomes 'FR'; 'ZZ', 'FRA' and a blank country give 400")
    void country_rules() throws Exception {
        String id = createCompany(mockMvc, "ACME Ltd", null);

        mockMvc.perform(json(post(URL + "/" + id + "/addresses"), address("BILLING", "Paris", "fr", false), "SALES"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.addresses[0].country").value("FR"));
        for (String bad : List.of("ZZ", "FRA", "F", " ")) {
            problem(mockMvc.perform(json(post(URL + "/" + id + "/addresses"), address("BILLING", "Paris", bad, false), "SALES")), 400)
                    .andExpect(jsonPath("$.errors[*].field").value(hasItem("country")));
        }
        assertThat((Integer) read(getCustomer(id), "$.addresses.length()")).isEqualTo(1);
    }

    @Test
    @DisplayName("AC-8: a missing type, line1, city or country gives 400 with the field names, and stores nothing")
    void invalid_address_is_400() throws Exception {
        String id = createCompany(mockMvc, "ACME Ltd", null);

        problem(mockMvc.perform(json(post(URL + "/" + id + "/addresses"), "{}", "SALES")), 400)
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("type")))
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("line1")))
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("city")))
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("country")))
                .andExpect(content().string(not(containsString("Exception"))));
        problem(mockMvc.perform(json(post(URL + "/" + id + "/addresses"),
                "{\"type\":\"HOME\",\"line1\":\"l\",\"city\":\"c\",\"country\":\"FR\"}", "SALES")), 400);
        assertThat((Integer) read(getCustomer(id), "$.version")).isZero();
    }

    @Test
    @DisplayName("AC-8: unknown customer or unknown address gives 404")
    void unknown_ids() throws Exception {
        String id = createCompany(mockMvc, "ACME Ltd", null);

        problem(mockMvc.perform(json(post(URL + "/" + UNKNOWN + "/addresses"), address("BILLING", "Paris", "FR", false), "SALES")), 404);
        problem(mockMvc.perform(json(put(URL + "/" + id + "/addresses/" + UNKNOWN), address("BILLING", "Paris", "FR", false), "SALES")), 404);
        problem(mockMvc.perform(delete(URL + "/" + id + "/addresses/" + UNKNOWN).header(HttpHeaders.AUTHORIZATION, bearer("SALES"))), 404);
    }

    @Test
    @DisplayName("AC-8: an inactive customer refuses address changes (409), and accepts them after reactivation")
    void inactive_customer_refuses_changes() throws Exception {
        String id = createCompany(mockMvc, "ACME Ltd", null);
        String addressId = read(addAddress(id, address("BILLING", "Paris", "FR", false)), "$.addresses[0].id");
        mockMvc.perform(json(post(URL + "/" + id + "/deactivate"), "", "SALES")).andExpect(status().isOk());

        problem(mockMvc.perform(json(post(URL + "/" + id + "/addresses"), address("SHIPPING", "Lyon", "FR", false), "SALES")), 409);
        problem(mockMvc.perform(json(put(URL + "/" + id + "/addresses/" + addressId), address("BILLING", "Nice", "FR", false), "SALES")), 409);
        problem(mockMvc.perform(delete(URL + "/" + id + "/addresses/" + addressId).header(HttpHeaders.AUTHORIZATION, bearer("SALES"))), 409);
        assertThat((Integer) read(getCustomer(id), "$.addresses.length()")).isEqualTo(1);

        mockMvc.perform(json(post(URL + "/" + id + "/reactivate"), "", "SALES")).andExpect(status().isOk());
        mockMvc.perform(json(post(URL + "/" + id + "/addresses"), address("SHIPPING", "Lyon", "FR", false), "SALES"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("AC-8: contacts and addresses live together on one customer, and a customer update keeps both")
    void contacts_and_addresses_together() throws Exception {
        String id = createCompany(mockMvc, "ACME Ltd", "FR1");
        addAddress(id, address("BILLING", "Paris", "FR", true));
        mockMvc.perform(json(post(URL + "/" + id + "/contacts"),
                        "{\"firstName\":\"Jane\",\"lastName\":\"Doe\",\"primary\":true}", "SALES"))
                .andExpect(status().isCreated());

        mockMvc.perform(json(put(URL + "/" + id),
                        "{\"version\":2,\"type\":\"COMPANY\",\"name\":\"ACME Group\",\"vatNumber\":\"FR1\"}", "SALES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(3))
                .andExpect(jsonPath("$.contacts.length()").value(1))
                .andExpect(jsonPath("$.addresses.length()").value(1));
    }

    @Test
    @DisplayName("AC-8: the database allows only one default address per customer and type (last guard behind the domain)")
    void database_guards_the_default_address() throws Exception {
        String id = createCompany(mockMvc, "ACME Ltd", null);
        String insert = """
                INSERT INTO customer_address (id, version, customer_id, type, line1, city, country, default_for_type)
                VALUES (gen_random_uuid(), 0, '%s', '%s', 'l1', 'Paris', 'FR', %s)""";
        jdbc.update(insert.formatted(id, "BILLING", "true"));
        jdbc.update(insert.formatted(id, "SHIPPING", "true"));
        jdbc.update(insert.formatted(id, "BILLING", "false"));

        assertThatThrownBy(() -> jdbc.update(insert.formatted(id, "BILLING", "true")))
                .hasMessageContaining("ux_customer_address_default");
    }

    @Test
    @DisplayName("AC-8: the database refuses a country that is not two capital letters")
    void database_guards_the_country() throws Exception {
        String id = createCompany(mockMvc, "ACME Ltd", null);

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO customer_address (id, version, customer_id, type, line1, city, country, default_for_type)
                VALUES (gen_random_uuid(), 0, ?::uuid, 'BILLING', 'l1', 'Paris', 'fr', false)""", id))
                .hasMessageContaining("ck_customer_address_country");
    }
}
