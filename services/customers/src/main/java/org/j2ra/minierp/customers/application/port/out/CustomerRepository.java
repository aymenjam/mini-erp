package org.j2ra.minierp.customers.application.port.out;

import org.j2ra.minierp.customers.domain.Customer;
import org.j2ra.minierp.customers.domain.PageQuery;
import org.j2ra.minierp.customers.domain.PageResult;

import java.util.Optional;
import java.util.UUID;

/** Output port: where customers are stored. Infrastructure implements it. */
public interface CustomerRepository {

    Optional<Customer> findById(UUID id);

    /**
     * Inserts a new customer or updates an existing one, and writes to the database at once.
     * Throws DuplicateVatNumberException or CustomerVersionConflictException when the database refuses the change.
     * Returns the customer as stored (with its new version).
     */
    Customer save(Customer customer);

    PageResult<Customer> findAll(PageQuery query);
}
