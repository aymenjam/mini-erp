package org.j2ra.minierp.customers.infrastructure.out.persistence;

import org.j2ra.minierp.customers.application.port.out.CustomerRepository;
import org.j2ra.minierp.customers.domain.Customer;
import org.j2ra.minierp.customers.domain.PageQuery;
import org.j2ra.minierp.customers.domain.PageResult;

import java.util.List;

/**
 * Stub: there is no customer table yet, so the list is always empty.
 * Roadmap item 1.1 replaces it with a JPA entity, a Spring Data repository and a mapper.
 */
public class CustomerPersistenceAdapter implements CustomerRepository {

    @Override
    public PageResult<Customer> findAll(PageQuery query) {
        return new PageResult<>(List.of(), query.page(), query.size(), 0, 0);
    }
}
