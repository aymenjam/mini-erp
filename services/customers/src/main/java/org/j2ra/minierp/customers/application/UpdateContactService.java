package org.j2ra.minierp.customers.application;

import org.j2ra.minierp.customers.application.port.in.UpdateContactUseCase;
import org.j2ra.minierp.customers.application.port.out.CustomerRepository;
import org.j2ra.minierp.customers.domain.Contact;
import org.j2ra.minierp.customers.domain.Customer;
import org.j2ra.minierp.customers.domain.CustomerNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/** Not a Spring bean: infrastructure.config creates use cases. */
@Transactional
public class UpdateContactService implements UpdateContactUseCase {

    private final CustomerRepository repository;
    private final Clock clock;

    public UpdateContactService(CustomerRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public Customer update(Command command) {
        Customer customer = repository.findById(command.customerId())
                .orElseThrow(() -> new CustomerNotFoundException(command.customerId()));
        Contact contact = new Contact(command.contactId(), command.firstName(), command.lastName(), command.email(),
                command.phone(), command.jobTitle(), command.primary());
        return repository.save(customer.replaceContact(contact, clock.instant()));
    }
}
