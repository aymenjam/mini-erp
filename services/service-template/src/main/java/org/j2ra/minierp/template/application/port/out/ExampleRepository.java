package org.j2ra.minierp.template.application.port.out;

import org.j2ra.minierp.template.domain.ExampleValue;

import java.util.Optional;

/** Example output port: what a use case needs from outside. Infrastructure implements it. Delete it in a real service. */
public interface ExampleRepository {

    Optional<ExampleValue> load();
}
