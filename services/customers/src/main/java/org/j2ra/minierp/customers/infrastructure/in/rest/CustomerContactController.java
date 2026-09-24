package org.j2ra.minierp.customers.infrastructure.in.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.j2ra.minierp.customers.application.port.in.AddContactUseCase;
import org.j2ra.minierp.customers.application.port.in.RemoveContactUseCase;
import org.j2ra.minierp.customers.application.port.in.UpdateContactUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/**
 * Contacts are part of the customer. Every answer is the whole customer, so the caller gets its new version
 * (each change adds 1) and does not need a second call.
 */
@RestController
@RequestMapping("/api/v1/customers/{customerId}/contacts")
@PreAuthorize("hasAnyRole('SALES', 'ADMIN')")
class CustomerContactController {

    private static final String PROBLEM_SCHEMA = "#/components/schemas/ProblemDetail";

    private final AddContactUseCase addContact;
    private final UpdateContactUseCase updateContact;
    private final RemoveContactUseCase removeContact;

    CustomerContactController(AddContactUseCase addContact, UpdateContactUseCase updateContact,
                              RemoveContactUseCase removeContact) {
        this.addContact = addContact;
        this.updateContact = updateContact;
        this.removeContact = removeContact;
    }

    @Operation(summary = "Add a contact to a customer",
            description = "Needs the role SALES or ADMIN. If `primary` is true, the contact that was primary is not primary any more. "
                    + "The answer is the whole customer, with its new version. An inactive customer cannot be changed.")
    @ApiResponse(responseCode = "201", description = "The contact was added. The `Location` header is its URL")
    @ApiResponse(responseCode = "400", description = "A field is not valid. The `errors` list names the fields",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(ref = PROBLEM_SCHEMA)))
    @ApiResponse(responseCode = "404", description = "No customer with this id",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(ref = PROBLEM_SCHEMA)))
    @ApiResponse(responseCode = "409", description = "The customer is inactive, or someone changed it at the same time",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(ref = PROBLEM_SCHEMA)))
    @PostMapping
    ResponseEntity<CustomerResponse> add(@PathVariable("customerId") UUID customerId, @Valid @RequestBody ContactRequest request) {
        AddContactUseCase.Result result = addContact.add(request.toAddCommand(customerId));
        URI location = URI.create("/api/v1/customers/" + customerId + "/contacts/" + result.contactId());
        return ResponseEntity.created(location).body(CustomerResponse.from(result.customer()));
    }

    @Operation(summary = "Replace a contact of a customer",
            description = "Needs the role SALES or ADMIN. All fields of the contact are replaced. If `primary` is true, "
                    + "the contact that was primary is not primary any more. The answer is the whole customer, with its new version.")
    @ApiResponse(responseCode = "200", description = "The contact was changed")
    @ApiResponse(responseCode = "400", description = "A field is not valid. The `errors` list names the fields",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(ref = PROBLEM_SCHEMA)))
    @ApiResponse(responseCode = "404", description = "No customer or no contact with this id",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(ref = PROBLEM_SCHEMA)))
    @ApiResponse(responseCode = "409", description = "The customer is inactive, or someone changed it at the same time",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(ref = PROBLEM_SCHEMA)))
    @PutMapping("/{contactId}")
    CustomerResponse update(@PathVariable("customerId") UUID customerId, @PathVariable("contactId") UUID contactId,
                            @Valid @RequestBody ContactRequest request) {
        return CustomerResponse.from(updateContact.update(request.toUpdateCommand(customerId, contactId)));
    }

    @Operation(summary = "Remove a contact from a customer",
            description = "Needs the role SALES or ADMIN. The answer is the whole customer, with its new version.")
    @ApiResponse(responseCode = "200", description = "The contact was removed")
    @ApiResponse(responseCode = "404", description = "No customer or no contact with this id",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(ref = PROBLEM_SCHEMA)))
    @ApiResponse(responseCode = "409", description = "The customer is inactive, or someone changed it at the same time",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(ref = PROBLEM_SCHEMA)))
    @DeleteMapping("/{contactId}")
    CustomerResponse remove(@PathVariable("customerId") UUID customerId, @PathVariable("contactId") UUID contactId) {
        return CustomerResponse.from(removeContact.remove(customerId, contactId));
    }
}
