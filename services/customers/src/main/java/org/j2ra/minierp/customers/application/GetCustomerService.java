package org.j2ra.minierp.customers.application;

import org.j2ra.minierp.customers.application.port.in.GetCustomerUseCase;
import org.j2ra.minierp.customers.application.port.out.CustomerRepository;
import org.j2ra.minierp.customers.domain.Customer;
import org.j2ra.minierp.customers.domain.CustomerNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Not a Spring bean: infrastructure.config creates use cases. */
@Transactional(readOnly = true)
public class GetCustomerService implements GetCustomerUseCase {

    private final CustomerRepository repository;

    public GetCustomerService(CustomerRepository repository) {
        this.repository = repository;
    }

    @Override
    public Customer get(UUID id) {
        return repository.findById(id).orElseThrow(() -> new CustomerNotFoundException(id));
    }
}
