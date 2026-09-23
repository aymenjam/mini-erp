package org.j2ra.minierp.customers.application;

import org.j2ra.minierp.customers.application.port.in.UpdateCustomerUseCase;
import org.j2ra.minierp.customers.application.port.out.CustomerRepository;
import org.j2ra.minierp.customers.domain.Customer;
import org.j2ra.minierp.customers.domain.CustomerNotFoundException;
import org.j2ra.minierp.customers.domain.CustomerVersionConflictException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/** Not a Spring bean: infrastructure.config creates use cases. */
@Transactional
public class UpdateCustomerService implements UpdateCustomerUseCase {

    private final CustomerRepository repository;
    private final Clock clock;

    public UpdateCustomerService(CustomerRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public Customer update(Command command) {
        Customer existing = repository.findById(command.id()).orElseThrow(() -> new CustomerNotFoundException(command.id()));
        // Early answer for a caller with an old copy. A change that runs at the same time is stopped by @Version in the database.
        if (existing.version() != command.version()) {
            throw new CustomerVersionConflictException(command.id());
        }
        Customer changed = existing.update(command.type(), command.name(), command.email(), command.phone(),
                command.vatNumber(), clock.instant());
        return repository.save(changed);
    }
}
