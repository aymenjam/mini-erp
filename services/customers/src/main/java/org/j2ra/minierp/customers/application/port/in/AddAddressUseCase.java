package org.j2ra.minierp.customers.application.port.in;

import org.j2ra.minierp.customers.domain.AddressType;
import org.j2ra.minierp.customers.domain.Customer;

import java.util.UUID;

/** Input port: add an address to a customer. Returns the whole customer (with its new version). */
public interface AddAddressUseCase {

    Result add(Command command);

    record Result(Customer customer, UUID addressId) {
    }

    record Command(UUID customerId, AddressType type, String line1, String line2, String postalCode, String city,
                   String country, boolean defaultForType) {
    }
}
