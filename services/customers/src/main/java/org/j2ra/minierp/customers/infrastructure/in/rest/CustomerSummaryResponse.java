package org.j2ra.minierp.customers.infrastructure.in.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import org.j2ra.minierp.customers.domain.Customer;
import org.j2ra.minierp.customers.domain.CustomerStatus;
import org.j2ra.minierp.customers.domain.CustomerType;

import java.util.UUID;

@Schema(description = "A customer in a list: the main fields only")
record CustomerSummaryResponse(
        @Schema(description = "Id of the customer") UUID id,
        @Schema(description = "Company or private person", example = "COMPANY") CustomerType type,
        @Schema(description = "Company name, or full name of a private person", example = "ACME Ltd") String name,
        @Schema(description = "Email address", example = "contact@acme.example", nullable = true) String email,
        @Schema(description = "ACTIVE, or INACTIVE when the customer was deactivated", example = "ACTIVE") CustomerStatus status) {

    static CustomerSummaryResponse from(Customer customer) {
        return new CustomerSummaryResponse(customer.id(), customer.type(), customer.name(), customer.email(), customer.status());
    }
}
