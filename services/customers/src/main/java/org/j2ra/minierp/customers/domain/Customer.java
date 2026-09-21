package org.j2ra.minierp.customers.domain;

import java.util.UUID;

/** A customer. Only the id for now: the data fields come with roadmap item 1.1. */
public record Customer(UUID id) {

    public Customer {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
    }
}
