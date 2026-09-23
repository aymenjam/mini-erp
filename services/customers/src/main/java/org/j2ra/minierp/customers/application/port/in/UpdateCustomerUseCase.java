package org.j2ra.minierp.customers.application.port.in;

import org.j2ra.minierp.customers.domain.Customer;
import org.j2ra.minierp.customers.domain.CustomerType;

import java.util.UUID;

/** Input port: replace the main fields of a customer. The caller sends the version it read (optimistic locking). */
public interface UpdateCustomerUseCase {

    Customer update(Command command);

    record Command(UUID id, long version, CustomerType type, String name, String email, String phone, String vatNumber) {
    }
}
