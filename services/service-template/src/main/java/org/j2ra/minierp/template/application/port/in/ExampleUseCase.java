package org.j2ra.minierp.template.application.port.in;

import org.j2ra.minierp.template.domain.ExampleValue;

import java.util.Optional;

/** Example input port: what the outside may ask the application to do. Delete it in a real service. */
public interface ExampleUseCase {

    Optional<ExampleValue> show();
}
