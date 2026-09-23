package org.j2ra.minierp.customers.domain;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * A customer of the company that uses the ERP. Immutable: a change returns a new object.
 * All business rules are in the constructor, so an invalid customer cannot exist (also when it is read from the database).
 * Contacts and addresses come in slice 2 of spec 1.1.
 */
public record Customer(
        UUID id,
        CustomerType type,
        String name,
        String email,
        String phone,
        String vatNumber,
        CustomerStatus status,
        Instant createdAt,
        Instant updatedAt,
        long version) {

    public static final int NAME_MAX = 200;
    public static final int EMAIL_MAX = 254;
    public static final int PHONE_MAX = 50;
    public static final int VAT_NUMBER_MAX = 50;

    public Customer {
        if (id == null || status == null || createdAt == null || updatedAt == null) {
            throw new IllegalArgumentException("id, status, createdAt and updatedAt must not be null");
        }
        if (type == null) {
            throw new InvalidCustomerException("type", "must not be null");
        }
        name = clean(name);
        if (name == null) {
            throw new InvalidCustomerException("name", "must not be blank");
        }
        email = clean(email);
        phone = clean(phone);
        // Same VAT number written in two ways must not become two customers: we trim and use upper case.
        vatNumber = cleanUpper(vatNumber);
        tooLong("name", name, NAME_MAX);
        tooLong("email", email, EMAIL_MAX);
        tooLong("phone", phone, PHONE_MAX);
        tooLong("vatNumber", vatNumber, VAT_NUMBER_MAX);
        if (vatNumber != null && type != CustomerType.COMPANY) {
            throw new InvalidCustomerException("vatNumber", "only a company can have a VAT number");
        }
    }

    /** A new active customer with a new random id (UUID v4). */
    public static Customer create(CustomerType type, String name, String email, String phone, String vatNumber, Instant now) {
        return new Customer(UUID.randomUUID(), type, name, email, phone, vatNumber, CustomerStatus.ACTIVE, now, now, 0);
    }

    /** Replaces the main fields. An inactive customer must be reactivated first. */
    public Customer update(CustomerType newType, String newName, String newEmail, String newPhone, String newVatNumber, Instant now) {
        if (status == CustomerStatus.INACTIVE) {
            throw new CustomerInactiveException(id);
        }
        return new Customer(id, newType, newName, newEmail, newPhone, newVatNumber, status, createdAt, now, version);
    }

    /** Deactivating an inactive customer changes nothing. */
    public Customer deactivate(Instant now) {
        return status == CustomerStatus.INACTIVE ? this : withStatus(CustomerStatus.INACTIVE, now);
    }

    /** Reactivating an active customer changes nothing. */
    public Customer reactivate(Instant now) {
        return status == CustomerStatus.ACTIVE ? this : withStatus(CustomerStatus.ACTIVE, now);
    }

    private Customer withStatus(CustomerStatus newStatus, Instant now) {
        return new Customer(id, type, name, email, phone, vatNumber, newStatus, createdAt, now, version);
    }

    /** Trimmed text, or null when it is null or blank. */
    private static String clean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String cleanUpper(String value) {
        String cleaned = clean(value);
        return cleaned == null ? null : cleaned.toUpperCase(Locale.ROOT);
    }

    private static void tooLong(String field, String value, int max) {
        if (value != null && value.length() > max) {
            throw new InvalidCustomerException(field, "must have at most " + max + " characters");
        }
    }
}
