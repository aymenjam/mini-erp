package org.j2ra.minierp.customers.domain;

import java.util.UUID;

/** A person to contact at a customer. Part of the Customer aggregate. */
public record Contact(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String jobTitle,
        boolean primary) {

    public static final int NAME_MAX = 100;
    public static final int EMAIL_MAX = 254;
    public static final int PHONE_MAX = 50;
    public static final int JOB_TITLE_MAX = 100;

    public Contact {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        firstName = Fields.required("firstName", firstName, NAME_MAX);
        lastName = Fields.required("lastName", lastName, NAME_MAX);
        email = Fields.clean(email);
        phone = Fields.clean(phone);
        jobTitle = Fields.clean(jobTitle);
        Fields.tooLong("email", email, EMAIL_MAX);
        Fields.tooLong("phone", phone, PHONE_MAX);
        Fields.tooLong("jobTitle", jobTitle, JOB_TITLE_MAX);
    }

    /** A new contact with a new random id (UUID v4). */
    public static Contact create(String firstName, String lastName, String email, String phone, String jobTitle, boolean primary) {
        return new Contact(UUID.randomUUID(), firstName, lastName, email, phone, jobTitle, primary);
    }

    Contact withPrimary(boolean value) {
        return new Contact(id, firstName, lastName, email, phone, jobTitle, value);
    }
}
