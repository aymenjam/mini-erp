package org.j2ra.minierp.customers.application.port.in;

import org.j2ra.minierp.customers.domain.CustomerSummary;
import org.j2ra.minierp.customers.domain.PageQuery;
import org.j2ra.minierp.customers.domain.PageResult;

/** Input port: list customers, one page at a time. */
public interface ListCustomersUseCase {

    PageResult<CustomerSummary> list(PageQuery query);
}
