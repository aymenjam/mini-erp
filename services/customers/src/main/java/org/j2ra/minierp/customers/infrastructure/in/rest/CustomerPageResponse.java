package org.j2ra.minierp.customers.infrastructure.in.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import org.j2ra.minierp.customers.domain.Customer;
import org.j2ra.minierp.customers.domain.PageResult;

import java.util.List;

@Schema(description = "One page of customers")
record CustomerPageResponse(
        @Schema(description = "The customers of this page") List<CustomerSummaryResponse> content,
        @Schema(description = "Number of this page, counted from 0", example = "0") int page,
        @Schema(description = "Maximum number of customers in a page", example = "20") int size,
        @Schema(description = "Number of customers in all pages", example = "0") long totalElements,
        @Schema(description = "Number of pages", example = "0") int totalPages) {

    static CustomerPageResponse from(PageResult<Customer> result) {
        return new CustomerPageResponse(
                result.content().stream().map(CustomerSummaryResponse::from).toList(),
                result.page(), result.size(), result.totalElements(), result.totalPages());
    }
}
