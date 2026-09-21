package org.j2ra.minierp.template.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Domain test: plain JUnit, no Spring (constitution.md, section 5). */
class ExampleValueTest {

    @Test
    void keeps_the_text() {
        assertThat(new ExampleValue("hello").text()).isEqualTo("hello");
    }

    @Test
    void rejects_a_blank_text() {
        assertThatThrownBy(() -> new ExampleValue(" ")).isInstanceOf(IllegalArgumentException.class);
    }
}
