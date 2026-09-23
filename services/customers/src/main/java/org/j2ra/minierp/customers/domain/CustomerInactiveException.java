package org.j2ra.minierp.customers.domain;

import java.util.UUID;

/** An inactive customer cannot be changed. It must be reactivated first. */
public class CustomerInactiveException extends RuntimeException {

    public CustomerInactiveException(UUID id) {
        super("Customer " + id + " is inactive");
    }
}
