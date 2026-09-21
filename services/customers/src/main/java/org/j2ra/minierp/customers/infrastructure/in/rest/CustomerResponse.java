package org.j2ra.minierp.customers.infrastructure.in.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import org.j2ra.minierp.customers.domain.Customer;

import java.util.UUID;

@Schema(description = "A customer as the API shows it. Only the id for now.")
record CustomerResponse(@Schema(description = "Id of the customer") UUID id) {

    static CustomerResponse from(Customer customer) {
        return new CustomerResponse(customer.id());
    }
}
