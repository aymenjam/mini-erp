package org.j2ra.minierp.customers.domain;

/** Another customer already has this VAT number. The database constraint is the final judge, also when two requests run at once. */
public class DuplicateVatNumberException extends RuntimeException {

    public DuplicateVatNumberException() {
        super("A customer with this VAT number already exists");
    }
}
