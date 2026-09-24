package org.j2ra.minierp.customers.application.port.in;

import org.j2ra.minierp.customers.domain.AddressType;
import org.j2ra.minierp.customers.domain.Customer;

import java.util.UUID;

/** Input port: replace an address of a customer. Returns the whole customer (with its new version). */
public interface UpdateAddressUseCase {

    Customer update(Command command);

    record Command(UUID customerId, UUID addressId, AddressType type, String line1, String line2, String postalCode,
                   String city, String country, boolean defaultForType) {
    }
}
