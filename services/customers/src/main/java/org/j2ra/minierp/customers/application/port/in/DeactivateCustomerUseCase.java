package org.j2ra.minierp.customers.application.port.in;

import org.j2ra.minierp.customers.domain.Customer;

import java.util.UUID;

/** Input port: make a customer inactive. */
public interface DeactivateCustomerUseCase {

    Customer deactivate(UUID id);
}
