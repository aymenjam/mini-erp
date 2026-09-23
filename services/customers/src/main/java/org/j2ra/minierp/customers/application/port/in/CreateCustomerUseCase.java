package org.j2ra.minierp.customers.application.port.in;

import org.j2ra.minierp.customers.domain.Customer;
import org.j2ra.minierp.customers.domain.CustomerType;

/** Input port: create a customer. */
public interface CreateCustomerUseCase {

    Customer create(Command command);

    record Command(CustomerType type, String name, String email, String phone, String vatNumber) {
    }
}
