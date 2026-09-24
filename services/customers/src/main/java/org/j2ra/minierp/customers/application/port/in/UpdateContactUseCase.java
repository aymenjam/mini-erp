package org.j2ra.minierp.customers.application.port.in;

import org.j2ra.minierp.customers.domain.Customer;

import java.util.UUID;

/** Input port: replace a contact of a customer. Returns the whole customer (with its new version). */
public interface UpdateContactUseCase {

    Customer update(Command command);

    record Command(UUID customerId, UUID contactId, String firstName, String lastName, String email, String phone,
                   String jobTitle, boolean primary) {
    }
}
