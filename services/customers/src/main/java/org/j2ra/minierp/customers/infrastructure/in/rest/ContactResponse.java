package org.j2ra.minierp.customers.infrastructure.in.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import org.j2ra.minierp.customers.domain.Contact;

import java.util.UUID;

@Schema(description = "A contact of a customer")
record ContactResponse(
        @Schema(description = "Id of the contact") UUID id,
        @Schema(description = "First name", example = "Jane") String firstName,
        @Schema(description = "Last name", example = "Doe") String lastName,
        @Schema(description = "Email address", example = "jane.doe@acme.example", nullable = true) String email,
        @Schema(description = "Phone number", example = "+33 6 12 34 56 78", nullable = true) String phone,
        @Schema(description = "Job title", example = "Purchasing manager", nullable = true) String jobTitle,
        @Schema(description = "True for the main contact of the customer (at most one)", example = "true") boolean primary) {

    static ContactResponse from(Contact contact) {
        return new ContactResponse(contact.id(), contact.firstName(), contact.lastName(), contact.email(), contact.phone(),
                contact.jobTitle(), contact.primary());
    }
}
