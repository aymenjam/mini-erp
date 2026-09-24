package org.j2ra.minierp.customers.application;

import org.j2ra.minierp.customers.application.port.out.CustomerRepository;
import org.j2ra.minierp.customers.domain.Customer;
import org.j2ra.minierp.customers.domain.CustomerSummary;
import org.j2ra.minierp.customers.domain.PageQuery;
import org.j2ra.minierp.customers.domain.PageResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Fake output port for use case tests. It counts the writes, so a test can prove that nothing was written. */
class InMemoryCustomerRepository implements CustomerRepository {

    final Map<UUID, Customer> stored = new HashMap<>();
    int saves;

    @Override
    public Optional<Customer> findById(UUID id) {
        return Optional.ofNullable(stored.get(id));
    }

    @Override
    public Customer save(Customer customer) {
        saves++;
        stored.put(customer.id(), customer);
        return customer;
    }

    @Override
    public PageResult<CustomerSummary> findAll(PageQuery query) {
        List<CustomerSummary> summaries = stored.values().stream()
                .map(c -> new CustomerSummary(c.id(), c.type(), c.name(), c.email(), c.status())).toList();
        return new PageResult<>(summaries, query.page(), query.size(), stored.size(), 1);
    }
}
