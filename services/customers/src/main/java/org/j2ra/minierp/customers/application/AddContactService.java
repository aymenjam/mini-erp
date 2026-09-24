package org.j2ra.minierp.customers.application;

import org.j2ra.minierp.customers.application.port.in.AddContactUseCase;
import org.j2ra.minierp.customers.application.port.out.CustomerRepository;
import org.j2ra.minierp.customers.domain.Contact;
import org.j2ra.minierp.customers.domain.Customer;
import org.j2ra.minierp.customers.domain.CustomerNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/** Not a Spring bean: infrastructure.config creates use cases. */
@Transactional
public class AddContactService implements AddContactUseCase {

    private final CustomerRepository repository;
    private final Clock clock;

    public AddContactService(CustomerRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public Result add(Command command) {
        Customer customer = repository.findById(command.customerId())
                .orElseThrow(() -> new CustomerNotFoundException(command.customerId()));
        Contact contact = Contact.create(command.firstName(), command.lastName(), command.email(), command.phone(),
                command.jobTitle(), command.primary());
        Customer saved = repository.save(customer.addContact(contact, clock.instant()));
        return new Result(saved, contact.id());
    }
}
