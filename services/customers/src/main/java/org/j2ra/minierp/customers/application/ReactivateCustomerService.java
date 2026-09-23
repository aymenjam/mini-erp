package org.j2ra.minierp.customers.application;

import org.j2ra.minierp.customers.application.port.in.ReactivateCustomerUseCase;
import org.j2ra.minierp.customers.application.port.out.CustomerRepository;
import org.j2ra.minierp.customers.domain.Customer;
import org.j2ra.minierp.customers.domain.CustomerNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/** Not a Spring bean: infrastructure.config creates use cases. */
@Transactional
public class ReactivateCustomerService implements ReactivateCustomerUseCase {

    private final CustomerRepository repository;
    private final Clock clock;

    public ReactivateCustomerService(CustomerRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public Customer reactivate(UUID id) {
        Customer existing = repository.findById(id).orElseThrow(() -> new CustomerNotFoundException(id));
        Customer changed = existing.reactivate(clock.instant());
        return changed == existing ? existing : repository.save(changed);
    }
}
