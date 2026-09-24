package org.j2ra.minierp.customers.domain;

import java.util.Locale;

/** Small helpers to clean and check text fields. Shared by Customer, Contact and Address. */
final class Fields {

    private Fields() {
    }

    /** Trimmed text, or null when it is null or blank. */
    static String clean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    static String cleanUpper(String value) {
        String cleaned = clean(value);
        return cleaned == null ? null : cleaned.toUpperCase(Locale.ROOT);
    }

    /** A required text: trimmed, not blank, and not longer than max. */
    static String required(String field, String value, int max) {
        String cleaned = clean(value);
        if (cleaned == null) {
            throw new InvalidCustomerException(field, "must not be blank");
        }
        tooLong(field, cleaned, max);
        return cleaned;
    }

    static void tooLong(String field, String value, int max) {
        if (value != null && value.length() > max) {
            throw new InvalidCustomerException(field, "must have at most " + max + " characters");
        }
    }
}
