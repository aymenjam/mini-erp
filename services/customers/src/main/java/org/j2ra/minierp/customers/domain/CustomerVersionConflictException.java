package org.j2ra.minierp.customers.domain;

import java.util.UUID;

/** The customer was changed by someone else since the caller read it (optimistic locking). */
public class CustomerVersionConflictException extends RuntimeException {

    public CustomerVersionConflictException(UUID id) {
        super("Customer " + id + " was changed by someone else");
    }
}
