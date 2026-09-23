package org.j2ra.minierp.customers.application;

import org.j2ra.minierp.customers.application.port.in.CreateCustomerUseCase;
import org.j2ra.minierp.customers.application.port.out.CustomerRepository;
import org.j2ra.minierp.customers.domain.Customer;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/** Not a Spring bean: infrastructure.config creates use cases. */
@Transactional
public class CreateCustomerService implements CreateCustomerUseCase {

    private final CustomerRepository repository;
    private final Clock clock;

    public CreateCustomerService(CustomerRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public Customer create(Command command) {
        Customer customer = Customer.create(command.type(), command.name(), command.email(), command.phone(),
                command.vatNumber(), clock.instant());
        return repository.save(customer);
    }
}
