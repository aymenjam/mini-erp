package org.j2ra.minierp.template.application;

import org.j2ra.minierp.template.application.port.out.ExampleRepository;
import org.j2ra.minierp.template.domain.ExampleValue;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/** Use case test with a fake output port: no Spring, no database (constitution.md, section 5). */
class ExampleServiceTest {

    @Test
    void shows_what_the_repository_has() {
        ExampleRepository fake = () -> Optional.of(new ExampleValue("stored"));
        assertThat(new ExampleService(fake).show()).contains(new ExampleValue("stored"));
    }

    @Test
    void shows_nothing_when_the_repository_is_empty() {
        ExampleRepository fake = Optional::empty;
        assertThat(new ExampleService(fake).show()).isEmpty();
    }
}
