package org.j2ra.minierp.customers.application;

import org.j2ra.minierp.customers.application.port.out.CustomerRepository;
import org.j2ra.minierp.customers.domain.Customer;
import org.j2ra.minierp.customers.domain.PageQuery;
import org.j2ra.minierp.customers.domain.PageResult;
import org.j2ra.minierp.customers.domain.SortDirection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** AC-10: use case test with a fake output port. No Spring. */
class ListCustomersServiceTest {

    /** Remembers the query and returns a fixed page. */
    private static class FakeCustomerRepository implements CustomerRepository {
        PageQuery received;
        final PageResult<Customer> answer;

        FakeCustomerRepository(PageResult<Customer> answer) {
            this.answer = answer;
        }

        @Override
        public PageResult<Customer> findAll(PageQuery query) {
            received = query;
            return answer;
        }
    }

    @Test
    @DisplayName("AC-10: the query goes to the port and the empty page comes back")
    void empty_page_is_returned() {
        var repository = new FakeCustomerRepository(new PageResult<>(List.of(), 0, 20, 0, 0));
        var query = new PageQuery(0, 20, "id", SortDirection.ASC);

        PageResult<Customer> result = new ListCustomersService(repository).list(query);

        assertThat(repository.received).isSameAs(query);
        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
        assertThat(result.totalPages()).isZero();
    }

    @Test
    @DisplayName("AC-10: a page with customers is returned unchanged")
    void page_with_customers_is_returned() {
        var customer = new Customer(UUID.randomUUID());
        var repository = new FakeCustomerRepository(new PageResult<>(List.of(customer), 1, 5, 6, 2));

        PageResult<Customer> result = new ListCustomersService(repository).list(new PageQuery(1, 5, "id", SortDirection.DESC));

        assertThat(result).isSameAs(repository.answer);
        assertThat(result.content()).containsExactly(customer);
    }
}
