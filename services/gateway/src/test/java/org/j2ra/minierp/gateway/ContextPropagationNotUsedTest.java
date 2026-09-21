package org.j2ra.minierp.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ADR-010 (option A): the correlation id is written by our own log helper. The Micrometer context-propagation library
 * is not used. If somebody adds it, a decision (ADR) is needed first: it changes every log line and adds hooks to Reactor.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "ISSUER_URI=http://issuer.test/realms/mini-erp",
        "JWK_SET_URI=http://issuer.test/certs",
        "CUSTOMERS_URI=http://customers.test:8091",
        "CORS_ALLOWED_ORIGIN=http://localhost:4200"})
class ContextPropagationNotUsedTest {

    @Autowired
    private Environment environment;

    @Test
    @DisplayName("ADR-010: the Micrometer context-propagation library is not on the classpath of the gateway")
    void library_is_not_on_the_classpath() {
        assertThatThrownBy(() -> Class.forName("io.micrometer.context.ContextRegistry"))
                .isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    @DisplayName("ADR-010: automatic context propagation of Reactor is not switched on")
    void automatic_propagation_is_off() {
        assertThat(environment.getProperty("spring.reactor.context-propagation")).isNull();
    }
}
