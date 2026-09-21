package org.j2ra.minierp.template.domain;

/** Example domain object: pure Java. Delete it in a real service (docs/new-service.md). */
public record ExampleValue(String text) {

    public ExampleValue {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text must not be blank");
        }
    }
}
