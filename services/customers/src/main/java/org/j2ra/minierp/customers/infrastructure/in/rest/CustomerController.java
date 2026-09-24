package org.j2ra.minierp.customers.infrastructure.in.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.j2ra.minierp.customers.application.port.in.CreateCustomerUseCase;
import org.j2ra.minierp.customers.application.port.in.DeactivateCustomerUseCase;
import org.j2ra.minierp.customers.application.port.in.GetCustomerUseCase;
import org.j2ra.minierp.customers.application.port.in.ListCustomersUseCase;
import org.j2ra.minierp.customers.application.port.in.ReactivateCustomerUseCase;
import org.j2ra.minierp.customers.application.port.in.UpdateCustomerUseCase;
import org.j2ra.minierp.customers.domain.PageQuery;
import org.j2ra.minierp.customers.domain.SortDirection;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Locale;
import java.util.UUID;

// No @Validated on the class: it would switch to AOP validation, and a bad value would end as 500.
// The constraints on the parameters and @Valid on the bodies are enough. Spring MVC answers 400 Problem Details (ApiExceptionHandler).
@RestController
@RequestMapping("/api/v1/customers")
@PreAuthorize("hasAnyRole('SALES', 'ADMIN')")
class CustomerController {

    // Only "id" may be sorted for now. name and createdAt come with the search (spec 1.1, slice 3).
    private static final String SORT_PATTERN = "id,(asc|desc)";
    private static final String PROBLEM_SCHEMA = "#/components/schemas/ProblemDetail";

    private final ListCustomersUseCase listCustomers;
    private final GetCustomerUseCase getCustomer;
    private final CreateCustomerUseCase createCustomer;
    private final UpdateCustomerUseCase updateCustomer;
    private final DeactivateCustomerUseCase deactivateCustomer;
    private final ReactivateCustomerUseCase reactivateCustomer;

    CustomerController(ListCustomersUseCase listCustomers, GetCustomerUseCase getCustomer,
                       CreateCustomerUseCase createCustomer, UpdateCustomerUseCase updateCustomer,
                       DeactivateCustomerUseCase deactivateCustomer, ReactivateCustomerUseCase reactivateCustomer) {
        this.listCustomers = listCustomers;
        this.getCustomer = getCustomer;
        this.createCustomer = createCustomer;
        this.updateCustomer = updateCustomer;
        this.deactivateCustomer = deactivateCustomer;
        this.reactivateCustomer = reactivateCustomer;
    }

    @Operation(summary = "List customers",
            description = "One page of customers. Needs the role SALES or ADMIN. `sort` is `field,asc|desc`; only `id` is allowed for now.")
    @ApiResponse(responseCode = "200", description = "A page of customers")
    @ApiResponse(responseCode = "400", description = "`page`, `size` or `sort` is not valid",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(ref = PROBLEM_SCHEMA)))
    @GetMapping
    CustomerPageResponse list(
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(name = "sort", defaultValue = "id,asc") @Pattern(regexp = SORT_PATTERN) String sort) {
        String[] parts = sort.split(",");
        PageQuery query = new PageQuery(page, size, parts[0], SortDirection.valueOf(parts[1].toUpperCase(Locale.ROOT)));
        return CustomerPageResponse.from(listCustomers.list(query));
    }

    @Operation(summary = "Create a customer",
            description = "Needs the role SALES or ADMIN. The new customer is ACTIVE with version 0. The `Location` header is its URL.")
    @ApiResponse(responseCode = "201", description = "The customer was created")
    @ApiResponse(responseCode = "400", description = "A field is not valid. The `errors` list names the fields",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(ref = PROBLEM_SCHEMA)))
    @ApiResponse(responseCode = "409", description = "Another customer has the same VAT number",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(ref = PROBLEM_SCHEMA)))
    @PostMapping
    ResponseEntity<CustomerResponse> create(@Valid @RequestBody CreateCustomerRequest request) {
        CustomerResponse body = CustomerResponse.from(createCustomer.create(request.toCommand()));
        // A relative address: the gateway may sit in front, and only the path is stable.
        return ResponseEntity.created(URI.create("/api/v1/customers/" + body.id())).body(body);
    }

    @Operation(summary = "Get one customer", description = "Needs the role SALES or ADMIN.")
    @ApiResponse(responseCode = "200", description = "The customer")
    @ApiResponse(responseCode = "404", description = "No customer with this id",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(ref = PROBLEM_SCHEMA)))
    @GetMapping("/{id}")
    CustomerResponse get(@PathVariable("id") UUID id) {
        return CustomerResponse.from(getCustomer.get(id));
    }

    @Operation(summary = "Update a customer",
            description = "Needs the role SALES or ADMIN. All main fields are replaced. Send the `version` you read: "
                    + "if someone changed the customer since, the answer is 409. An inactive customer cannot be changed.")
    @ApiResponse(responseCode = "200", description = "The customer was changed. Its version is one higher")
    @ApiResponse(responseCode = "400", description = "A field is not valid. The `errors` list names the fields",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(ref = PROBLEM_SCHEMA)))
    @ApiResponse(responseCode = "404", description = "No customer with this id",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(ref = PROBLEM_SCHEMA)))
    @ApiResponse(responseCode = "409", description = "Old version, VAT number of another customer, or the customer is inactive",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(ref = PROBLEM_SCHEMA)))
    @PutMapping("/{id}")
    CustomerResponse update(@PathVariable("id") UUID id, @Valid @RequestBody UpdateCustomerRequest request) {
        return CustomerResponse.from(updateCustomer.update(request.toCommand(id)));
    }

    @Operation(summary = "Deactivate a customer",
            description = "Needs the role SALES or ADMIN. A customer is never deleted. Deactivating an inactive customer changes nothing.")
    @ApiResponse(responseCode = "200", description = "The customer is INACTIVE")
    @ApiResponse(responseCode = "404", description = "No customer with this id",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(ref = PROBLEM_SCHEMA)))
    @PostMapping("/{id}/deactivate")
    CustomerResponse deactivate(@PathVariable("id") UUID id) {
        return CustomerResponse.from(deactivateCustomer.deactivate(id));
    }

    @Operation(summary = "Reactivate a customer",
            description = "Needs the role SALES or ADMIN. Reactivating an active customer changes nothing.")
    @ApiResponse(responseCode = "200", description = "The customer is ACTIVE")
    @ApiResponse(responseCode = "404", description = "No customer with this id",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(ref = PROBLEM_SCHEMA)))
    @PostMapping("/{id}/reactivate")
    CustomerResponse reactivate(@PathVariable("id") UUID id) {
        return CustomerResponse.from(reactivateCustomer.reactivate(id));
    }
}
