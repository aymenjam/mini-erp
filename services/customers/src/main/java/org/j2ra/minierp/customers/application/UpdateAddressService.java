package org.j2ra.minierp.customers.application;

import org.j2ra.minierp.customers.application.port.in.UpdateAddressUseCase;
import org.j2ra.minierp.customers.application.port.out.CustomerRepository;
import org.j2ra.minierp.customers.domain.Address;
import org.j2ra.minierp.customers.domain.Customer;
import org.j2ra.minierp.customers.domain.CustomerNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/** Not a Spring bean: infrastructure.config creates use cases. */
@Transactional
public class UpdateAddressService implements UpdateAddressUseCase {

    private final CustomerRepository repository;
    private final Clock clock;

    public UpdateAddressService(CustomerRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public Customer update(Command command) {
        Customer customer = repository.findById(command.customerId())
                .orElseThrow(() -> new CustomerNotFoundException(command.customerId()));
        Address address = new Address(command.addressId(), command.type(), command.line1(), command.line2(),
                command.postalCode(), command.city(), command.country(), command.defaultForType());
        return repository.save(customer.replaceAddress(address, clock.instant()));
    }
}
