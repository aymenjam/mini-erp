package org.j2ra.minierp.customers.domain;

/** A customer is never deleted: orders and invoices will point to it. It is deactivated instead. */
public enum CustomerStatus {
    ACTIVE,
    INACTIVE
}
