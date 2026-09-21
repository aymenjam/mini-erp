package org.j2ra.minierp.customers.domain;

import java.util.List;

/** One page of a list, with the totals the client needs to draw a pager. */
public record PageResult<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public PageResult {
        content = List.copyOf(content);
    }
}
