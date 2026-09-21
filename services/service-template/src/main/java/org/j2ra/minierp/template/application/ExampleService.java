package org.j2ra.minierp.template.application;

import org.j2ra.minierp.template.application.port.in.ExampleUseCase;
import org.j2ra.minierp.template.application.port.out.ExampleRepository;
import org.j2ra.minierp.template.domain.ExampleValue;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** Example use case. Not a Spring bean: infrastructure.config creates use cases. Delete it in a real service. */
@Transactional(readOnly = true)
public class ExampleService implements ExampleUseCase {

    private final ExampleRepository repository;

    public ExampleService(ExampleRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<ExampleValue> show() {
        return repository.load();
    }
}
