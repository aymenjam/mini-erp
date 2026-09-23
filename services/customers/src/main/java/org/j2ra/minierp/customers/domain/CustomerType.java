package org.j2ra.minierp.customers.domain;

/** A company (B2B) or a private person (B2C). Only a company can have a VAT number. */
public enum CustomerType {
    COMPANY,
    INDIVIDUAL
}
