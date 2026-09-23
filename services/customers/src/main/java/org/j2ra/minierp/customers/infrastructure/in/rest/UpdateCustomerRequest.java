package org.j2ra.minierp.customers.infrastructure.in.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.j2ra.minierp.customers.application.port.in.UpdateCustomerUseCase;
import org.j2ra.minierp.customers.domain.Customer;
import org.j2ra.minierp.customers.domain.CustomerType;

import java.util.UUID;

@Schema(description = "New values of a customer. All main fields are replaced.")
record UpdateCustomerRequest(
        @Schema(description = "The version you read. A different current version gives 409.", example = "0")
        @NotNull @PositiveOrZero Long version,
        @Schema(description = "Company or private person", example = "COMPANY") @NotNull CustomerType type,
        @Schema(description = "Company name, or full name of a private person", example = "ACME Ltd")
        @NotBlank @Size(max = Customer.NAME_MAX) String name,
        @Schema(description = "Email address", example = "contact@acme.example")
        @Email @Size(max = Customer.EMAIL_MAX) String email,
        @Schema(description = "Phone number", example = "+33 1 23 45 67 89") @Size(max = Customer.PHONE_MAX) String phone,
        @Schema(description = "VAT number, companies only", example = "FR12345678901")
        @Size(max = Customer.VAT_NUMBER_MAX) String vatNumber) {

    UpdateCustomerUseCase.Command toCommand(UUID id) {
        return new UpdateCustomerUseCase.Command(id, version, type, name, email, phone, vatNumber);
    }
}
