package org.j2ra.minierp.customers.domain;

import java.util.UUID;

public class AddressNotFoundException extends RuntimeException {

    public AddressNotFoundException(UUID addressId) {
        super("Address " + addressId + " not found");
    }
}
