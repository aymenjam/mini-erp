package org.j2ra.minierp.customers.application.port.in;

import org.j2ra.minierp.customers.domain.Customer;

import java.util.UUID;

/** Input port: add a contact to a customer. Returns the whole customer (with its new version) and the id of the new contact. */
public interface AddContactUseCase {

    Result add(Command command);

    record Result(Customer customer, UUID contactId) {
    }

    record Command(UUID customerId, String firstName, String lastName, String email, String phone, String jobTitle,
                   boolean primary) {
    }
}
