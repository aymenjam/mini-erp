package org.j2ra.minierp.customers.application;

import org.j2ra.minierp.customers.application.port.in.DeactivateCustomerUseCase;
import org.j2ra.minierp.customers.application.port.out.CustomerRepository;
import org.j2ra.minierp.customers.domain.Customer;
import org.j2ra.minierp.customers.domain.CustomerNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/** Not a Spring bean: infrastructure.config creates use cases. */
@Transactional
public class DeactivateCustomerService implements DeactivateCustomerUseCase {

    private final CustomerRepository repository;
    private final Clock clock;

    public DeactivateCustomerService(CustomerRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public Customer deactivate(UUID id) {
        Customer existing = repository.findById(id).orElseThrow(() -> new CustomerNotFoundException(id));
        Customer changed = existing.deactivate(clock.instant());
        // The domain returns the same object when nothing changes: no write, no new version.
        return changed == existing ? existing : repository.save(changed);
    }
}
