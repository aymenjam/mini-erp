package org.j2ra.minierp.customers.application;

import org.j2ra.minierp.customers.application.port.in.RemoveAddressUseCase;
import org.j2ra.minierp.customers.application.port.out.CustomerRepository;
import org.j2ra.minierp.customers.domain.Customer;
import org.j2ra.minierp.customers.domain.CustomerNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/** Not a Spring bean: infrastructure.config creates use cases. */
@Transactional
public class RemoveAddressService implements RemoveAddressUseCase {

    private final CustomerRepository repository;
    private final Clock clock;

    public RemoveAddressService(CustomerRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public Customer remove(UUID customerId, UUID addressId) {
        Customer customer = repository.findById(customerId).orElseThrow(() -> new CustomerNotFoundException(customerId));
        return repository.save(customer.removeAddress(addressId, clock.instant()));
    }
}
