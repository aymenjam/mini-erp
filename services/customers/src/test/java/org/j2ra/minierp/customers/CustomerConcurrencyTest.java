package org.j2ra.minierp.customers;

import org.j2ra.minierp.customers.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.j2ra.minierp.customers.support.CustomerRequests.URL;
import static org.j2ra.minierp.customers.support.CustomerRequests.address;
import static org.j2ra.minierp.customers.support.CustomerRequests.bearer;
import static org.j2ra.minierp.customers.support.CustomerRequests.company;
import static org.j2ra.minierp.customers.support.CustomerRequests.contact;
import static org.j2ra.minierp.customers.support.CustomerRequests.createCompany;
import static org.j2ra.minierp.customers.support.CustomerRequests.json;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/** Spec 1.1, AC-4 and AC-5: two requests at the same time, real transactions, real PostgreSQL. The database decides. */
class CustomerConcurrencyTest extends AbstractPostgresIntegrationTest {

    /** Starts all requests together and returns their HTTP statuses. */
    private List<Integer> runTogether(List<MockHttpServletRequestBuilder> requests) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(requests.size());
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Integer>> futures = new ArrayList<>();
            for (MockHttpServletRequestBuilder request : requests) {
                Callable<Integer> call = () -> {
                    start.await();
                    return mockMvc.perform(request).andReturn().getResponse().getStatus();
                };
                futures.add(pool.submit(call));
            }
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> future : futures) {
                statuses.add(future.get());
            }
            return statuses.stream().sorted().toList();
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("AC-4: two updates with the same version at the same time: one 200, one 409, and the customer has version 1")
    void two_updates_with_the_same_version() throws Exception {
        String id = createCompany(mockMvc, "ACME Ltd", "FR1");
        MockHttpServletRequestBuilder first = json(put(URL + "/" + id),
                "{\"version\":0,\"type\":\"COMPANY\",\"name\":\"Name A\",\"vatNumber\":\"FR1\"}", "SALES");
        MockHttpServletRequestBuilder second = json(put(URL + "/" + id),
                "{\"version\":0,\"type\":\"COMPANY\",\"name\":\"Name B\",\"vatNumber\":\"FR1\"}", "ADMIN");

        assertThat(runTogether(List.of(first, second))).containsExactly(200, 409);

        String json = mockMvc.perform(get(URL + "/" + id).header(HttpHeaders.AUTHORIZATION, bearer("SALES")))
                .andReturn().getResponse().getContentAsString();
        assertThat(json).contains("\"version\":1");
    }

    @Test
    @DisplayName("AC-5: two creations with the same VAT number at the same time: one 201, one 409, and one customer is stored")
    void two_creations_with_the_same_vat_number() throws Exception {
        assertThat(runTogether(List.of(
                json(post(URL), company("Twin A", "FR777"), "SALES"),
                json(post(URL), company("Twin B", "FR777"), "SALES"))))
                .containsExactly(201, 409);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM customer WHERE vat_number = 'FR777'", Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("AC-7: two contact adds at the same time on the same customer: one 201, one 409, and only one contact is added")
    void two_contact_adds_at_the_same_time() throws Exception {
        String id = createCompany(mockMvc, "ACME Ltd", null);

        assertThat(runTogether(List.of(
                json(post(URL + "/" + id + "/contacts"), contact("Jane", "Doe", false), "SALES"),
                json(post(URL + "/" + id + "/contacts"), contact("John", "Smith", false), "SALES"))))
                .containsExactly(201, 409);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM customer_contact", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT version FROM customer WHERE id = ?::uuid", Long.class, id)).isEqualTo(1L);
    }

    @Test
    @DisplayName("AC-7: two primary contacts added at the same time: never two primary contacts in the database")
    void two_primary_contacts_at_the_same_time() throws Exception {
        String id = createCompany(mockMvc, "ACME Ltd", null);

        List<Integer> statuses = runTogether(List.of(
                json(post(URL + "/" + id + "/contacts"), contact("Jane", "Doe", true), "SALES"),
                json(post(URL + "/" + id + "/contacts"), contact("John", "Smith", true), "SALES")));

        assertThat(statuses).containsExactly(201, 409);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM customer_contact WHERE is_primary", Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("AC-8: two default addresses of one type added at the same time: never two defaults in the database")
    void two_default_addresses_at_the_same_time() throws Exception {
        String id = createCompany(mockMvc, "ACME Ltd", null);

        List<Integer> statuses = runTogether(List.of(
                json(post(URL + "/" + id + "/addresses"), address("BILLING", "Paris", "FR", true), "SALES"),
                json(post(URL + "/" + id + "/addresses"), address("BILLING", "Nice", "FR", true), "SALES")));

        assertThat(statuses).containsExactly(201, 409);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM customer_address WHERE default_for_type", Integer.class)).isEqualTo(1);
    }
}
