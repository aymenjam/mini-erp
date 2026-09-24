package org.j2ra.minierp.customers.domain;

import java.util.UUID;

/** The main fields of a customer, for lists. It has no contacts and no addresses, so a list does not load them. */
public record CustomerSummary(UUID id, CustomerType type, String name, String email, CustomerStatus status) {
}
