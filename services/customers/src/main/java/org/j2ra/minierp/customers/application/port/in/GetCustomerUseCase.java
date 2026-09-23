package org.j2ra.minierp.customers.application.port.in;

import org.j2ra.minierp.customers.domain.Customer;

import java.util.UUID;

/** Input port: read one customer. Throws CustomerNotFoundException when the id is unknown. */
public interface GetCustomerUseCase {

    Customer get(UUID id);
}
