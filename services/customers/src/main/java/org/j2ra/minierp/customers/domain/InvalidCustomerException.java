package org.j2ra.minierp.customers.domain;

/** A business rule on the customer data is broken. It names the field, so the API can show it. */
public class InvalidCustomerException extends RuntimeException {

    private final String field;

    public InvalidCustomerException(String field, String message) {
        super(field + " " + message);
        this.field = field;
    }

    public String field() {
        return field;
    }

    /** The message without the field name. */
    public String reason() {
        return getMessage().substring(field.length() + 1);
    }
}
