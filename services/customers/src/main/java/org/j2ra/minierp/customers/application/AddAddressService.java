package org.j2ra.minierp.customers.application;

import org.j2ra.minierp.customers.application.port.in.AddAddressUseCase;
import org.j2ra.minierp.customers.application.port.out.CustomerRepository;
import org.j2ra.minierp.customers.domain.Address;
import org.j2ra.minierp.customers.domain.Customer;
import org.j2ra.minierp.customers.domain.CustomerNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/** Not a Spring bean: infrastructure.config creates use cases. */
@Transactional
public class AddAddressService implements AddAddressUseCase {

    private final CustomerRepository repository;
    private final Clock clock;

    public AddAddressService(CustomerRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public Result add(Command command) {
        Customer customer = repository.findById(command.customerId())
                .orElseThrow(() -> new CustomerNotFoundException(command.customerId()));
        Address address = Address.create(command.type(), command.line1(), command.line2(), command.postalCode(),
                command.city(), command.country(), command.defaultForType());
        Customer saved = repository.save(customer.addAddress(address, clock.instant()));
        return new Result(saved, address.id());
    }
}
