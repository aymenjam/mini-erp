package org.j2ra.minierp.customers.application.port.out;

import org.j2ra.minierp.customers.domain.Customer;
import org.j2ra.minierp.customers.domain.PageQuery;
import org.j2ra.minierp.customers.domain.PageResult;

/** Output port: where customers are stored. Infrastructure implements it. */
public interface CustomerRepository {

    PageResult<Customer> findAll(PageQuery query);
}
