package org.j2ra.minierp.customers.domain;

import java.util.UUID;

public class ContactNotFoundException extends RuntimeException {

    public ContactNotFoundException(UUID contactId) {
        super("Contact " + contactId + " not found");
    }
}
