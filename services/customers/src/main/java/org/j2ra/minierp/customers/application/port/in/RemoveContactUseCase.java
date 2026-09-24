package org.j2ra.minierp.customers.application.port.in;

import org.j2ra.minierp.customers.domain.Customer;

import java.util.UUID;

/** Input port: remove a contact from a customer. Returns the whole customer (with its new version). */
public interface RemoveContactUseCase {

    Customer remove(UUID customerId, UUID contactId);
}
