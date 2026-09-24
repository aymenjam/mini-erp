package org.j2ra.minierp.customers.application;

import org.j2ra.minierp.customers.application.port.in.ListCustomersUseCase;
import org.j2ra.minierp.customers.application.port.out.CustomerRepository;
import org.j2ra.minierp.customers.domain.CustomerSummary;
import org.j2ra.minierp.customers.domain.PageQuery;
import org.j2ra.minierp.customers.domain.PageResult;
import org.springframework.transaction.annotation.Transactional;

/** Not a Spring bean: infrastructure.config creates use cases. */
@Transactional(readOnly = true)
public class ListCustomersService implements ListCustomersUseCase {

    private final CustomerRepository repository;

    public ListCustomersService(CustomerRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<CustomerSummary> list(PageQuery query) {
        return repository.findAll(query);
    }
}
