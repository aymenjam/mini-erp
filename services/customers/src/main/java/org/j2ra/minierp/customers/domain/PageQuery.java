package org.j2ra.minierp.customers.domain;

/**
 * Which page of a list is asked for. The adapter that reads the request has already checked the limits
 * (size at most 100, allowed sort fields), because those are rules of the API.
 */
public record PageQuery(int page, int size, String sortField, SortDirection direction) {

    public PageQuery {
        if (sortField == null || sortField.isBlank()) {
            throw new IllegalArgumentException("sortField must not be blank");
        }
        if (direction == null) {
            throw new IllegalArgumentException("direction must not be null");
        }
    }
}
