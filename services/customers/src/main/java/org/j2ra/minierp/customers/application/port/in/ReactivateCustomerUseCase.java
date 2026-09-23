package org.j2ra.minierp.customers.application.port.in;

import org.j2ra.minierp.customers.domain.Customer;

import java.util.UUID;

/** Input port: make an inactive customer active again. */
public interface ReactivateCustomerUseCase {

    Customer reactivate(UUID id);
}
