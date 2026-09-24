package org.j2ra.minierp.customers.infrastructure.in.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.j2ra.minierp.customers.application.port.in.AddAddressUseCase;
import org.j2ra.minierp.customers.application.port.in.RemoveAddressUseCase;
import org.j2ra.minierp.customers.application.port.in.UpdateAddressUseCase;
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
 * Addresses are part of the customer. Every answer is the whole customer, so the caller gets its new version
 * (each change adds 1) and does not need a second call.
 */
@RestController
@RequestMapping("/api/v1/customers/{customerId}/addresses")
@PreAuthorize("hasAnyRole('SALES', 'ADMIN')")
class CustomerAddressController {

    private static final String PROBLEM_SCHEMA = "#/components/schemas/ProblemDetail";

    private final AddAddressUseCase addAddress;
    private final UpdateAddressUseCase updateAddress;
    private final RemoveAddressUseCase removeAddress;

    CustomerAddressController(AddAddressUseCase addAddress, UpdateAddressUseCase updateAddress,
                              RemoveAddressUseCase removeAddress) {
        this.addAddress = addAddress;
        this.updateAddress = updateAddress;
        this.removeAddress = removeAddress;
    }

    @Operation(summary = "Add an address to a customer",
            description = "Needs the role SALES or ADMIN. If `defaultForType` is true, the address that was the default for the same type is not the default any more. "
                    + "The answer is the whole customer, with its new version. An inactive customer cannot be changed.")
    @ApiResponse(responseCode = "201", description = "The address was added. The `Location` header is its URL")
    @ApiResponse(responseCode = "400", description = "A field is not valid. The `errors` list names the fields",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(ref = PROBLEM_SCHEMA)))
    @ApiResponse(responseCode = "404", description = "No customer with this id",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(ref = PROBLEM_SCHEMA)))
    @ApiResponse(responseCode = "409", description = "The customer is inactive, or someone changed it at the same time",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(ref = PROBLEM_SCHEMA)))
    @PostMapping
    ResponseEntity<CustomerResponse> add(@PathVariable("customerId") UUID customerId, @Valid @RequestBody AddressRequest request) {
        AddAddressUseCase.Result result = addAddress.add(request.toAddCommand(customerId));
        URI location = URI.create("/api/v1/customers/" + customerId + "/addresses/" + result.addressId());
        return ResponseEntity.created(location).body(CustomerResponse.from(result.customer()));
    }

    @Operation(summary = "Replace an address of a customer",
            description = "Needs the role SALES or ADMIN. All fields of the address are replaced. If `defaultForType` is true, "
                    + "the address that was the default for the same type is not the default any more. The answer is the whole customer, with its new version.")
    @ApiResponse(responseCode = "200", description = "The address was changed")
    @ApiResponse(responseCode = "400", description = "A field is not valid. The `errors` list names the fields",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(ref = PROBLEM_SCHEMA)))
    @ApiResponse(responseCode = "404", description = "No customer or no address with this id",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(ref = PROBLEM_SCHEMA)))
    @ApiResponse(responseCode = "409", description = "The customer is inactive, or someone changed it at the same time",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(ref = PROBLEM_SCHEMA)))
    @PutMapping("/{addressId}")
    CustomerResponse update(@PathVariable("customerId") UUID customerId, @PathVariable("addressId") UUID addressId,
                            @Valid @RequestBody AddressRequest request) {
        return CustomerResponse.from(updateAddress.update(request.toUpdateCommand(customerId, addressId)));
    }

    @Operation(summary = "Remove an address from a customer",
            description = "Needs the role SALES or ADMIN. The answer is the whole customer, with its new version.")
    @ApiResponse(responseCode = "200", description = "The address was removed")
    @ApiResponse(responseCode = "404", description = "No customer or no address with this id",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(ref = PROBLEM_SCHEMA)))
    @ApiResponse(responseCode = "409", description = "The customer is inactive, or someone changed it at the same time",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(ref = PROBLEM_SCHEMA)))
    @DeleteMapping("/{addressId}")
    CustomerResponse remove(@PathVariable("customerId") UUID customerId, @PathVariable("addressId") UUID addressId) {
        return CustomerResponse.from(removeAddress.remove(customerId, addressId));
    }
}
