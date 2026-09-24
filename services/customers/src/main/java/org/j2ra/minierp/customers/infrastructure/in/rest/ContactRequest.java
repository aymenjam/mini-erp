package org.j2ra.minierp.customers.infrastructure.in.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.j2ra.minierp.customers.application.port.in.AddContactUseCase;
import org.j2ra.minierp.customers.application.port.in.UpdateContactUseCase;
import org.j2ra.minierp.customers.domain.Contact;

import java.util.UUID;

// The limits repeat the domain ones on purpose: here they give a clear 400 with all bad fields at once.
@Schema(description = "Data of a contact. The same body is used to add a contact and to replace one.")
record ContactRequest(
        @Schema(description = "First name", example = "Jane") @NotBlank @Size(max = Contact.NAME_MAX) String firstName,
        @Schema(description = "Last name", example = "Doe") @NotBlank @Size(max = Contact.NAME_MAX) String lastName,
        @Schema(description = "Email address", example = "jane.doe@acme.example") @Email @Size(max = Contact.EMAIL_MAX) String email,
        @Schema(description = "Phone number", example = "+33 6 12 34 56 78") @Size(max = Contact.PHONE_MAX) String phone,
        @Schema(description = "Job title", example = "Purchasing manager") @Size(max = Contact.JOB_TITLE_MAX) String jobTitle,
        @Schema(description = "The main contact of the customer. If true, the contact that was primary is not primary any more. Default: false.",
                example = "true") boolean primary) {

    AddContactUseCase.Command toAddCommand(UUID customerId) {
        return new AddContactUseCase.Command(customerId, firstName, lastName, email, phone, jobTitle, primary);
    }

    UpdateContactUseCase.Command toUpdateCommand(UUID customerId, UUID contactId) {
        return new UpdateContactUseCase.Command(customerId, contactId, firstName, lastName, email, phone, jobTitle, primary);
    }
}
