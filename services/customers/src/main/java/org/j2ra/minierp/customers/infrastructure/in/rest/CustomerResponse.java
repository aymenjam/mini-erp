package org.j2ra.minierp.customers.infrastructure.in.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import org.j2ra.minierp.customers.domain.Customer;
import org.j2ra.minierp.customers.domain.CustomerStatus;
import org.j2ra.minierp.customers.domain.CustomerType;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "A customer as the API shows it")
record CustomerResponse(
        @Schema(description = "Id of the customer") UUID id,
        @Schema(description = "Company or private person", example = "COMPANY") CustomerType type,
        @Schema(description = "Company name, or full name of a private person", example = "ACME Ltd") String name,
        @Schema(description = "Email address", example = "contact@acme.example", nullable = true) String email,
        @Schema(description = "Phone number", example = "+33 1 23 45 67 89", nullable = true) String phone,
        @Schema(description = "VAT number (companies only)", example = "FR12345678901", nullable = true) String vatNumber,
        @Schema(description = "ACTIVE, or INACTIVE when the customer was deactivated", example = "ACTIVE") CustomerStatus status,
        @Schema(description = "Creation time (UTC)") Instant createdAt,
        @Schema(description = "Time of the last change (UTC)") Instant updatedAt,
        @Schema(description = "Version of the customer. Send it back when you update.", example = "0") long version) {

    static CustomerResponse from(Customer customer) {
        return new CustomerResponse(customer.id(), customer.type(), customer.name(), customer.email(), customer.phone(),
                customer.vatNumber(), customer.status(), customer.createdAt(), customer.updatedAt(), customer.version());
    }
}
