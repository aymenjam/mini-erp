package org.j2ra.minierp.customers.infrastructure.in.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.j2ra.minierp.customers.application.port.in.ListCustomersUseCase;
import org.j2ra.minierp.customers.domain.PageQuery;
import org.j2ra.minierp.customers.domain.SortDirection;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

// No @Validated on the class: it would switch to AOP validation, and a bad value would end as 500.
// The constraints on the parameters are enough. Spring MVC answers 400 Problem Details (ApiExceptionHandler).
@RestController
@RequestMapping("/api/v1/customers")
class CustomerController {

    // Only "id" may be sorted for now. A new sort field is added here, with the field on the customer (roadmap 1.1).
    private static final String SORT_PATTERN = "id,(asc|desc)";

    private final ListCustomersUseCase listCustomers;

    CustomerController(ListCustomersUseCase listCustomers) {
        this.listCustomers = listCustomers;
    }

    @Operation(summary = "List customers",
            description = "One page of customers. Needs the role SALES or ADMIN. `sort` is `field,asc|desc`; only `id` is allowed for now.")
    @ApiResponse(responseCode = "200", description = "A page of customers (empty for now)")
    @ApiResponse(responseCode = "400", description = "`page`, `size` or `sort` is not valid",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    @GetMapping
    @PreAuthorize("hasAnyRole('SALES', 'ADMIN')")
    CustomerPageResponse list(
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(name = "sort", defaultValue = "id,asc") @Pattern(regexp = SORT_PATTERN) String sort) {
        String[] parts = sort.split(",");
        PageQuery query = new PageQuery(page, size, parts[0], SortDirection.valueOf(parts[1].toUpperCase(Locale.ROOT)));
        return CustomerPageResponse.from(listCustomers.list(query));
    }
}
