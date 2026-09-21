package org.j2ra.minierp.gateway.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AC-22: every rule must be able to fail. Each rule runs on a wrong sample class, and must name it.
 * Without this test, a rule that never fails would look the same as a rule that works.
 * The samples are written as bytecode when the test runs (see SampleClasses), inside the package of the gateway.
 */
class GatewayArchitectureRulesCanFailTest {

    private static final String BASE = GatewayArchitectureTest.BASE;
    private static final String SAMPLES = BASE + ".samples";

    @TempDir
    Path folder;

    private void assertRejects(ArchRule rule, String badClass, String forbiddenType) {
        JavaClasses classes = SampleClasses.classWithFieldOfType(folder, SAMPLES, badClass, forbiddenType);
        assertThat(classes).as("the sample class was written").isNotEmpty();
        assertThatThrownBy(() -> rule.check(classes))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining(badClass);
    }

    @Test
    @DisplayName("AC-22: the rule for other modules rejects a class that uses a service class")
    void rejects_dependency_on_a_service() {
        assertRejects(GatewayArchitectureRules.noDependencyOnOtherModules(BASE),
                "BadUsesCustomers", "org.j2ra.minierp.customers.domain.Customer");
    }

    @Test
    @DisplayName("AC-22: the rule for other modules accepts a class of the gateway itself and a JDK class")
    void accepts_own_and_jdk_classes() {
        JavaClasses classes = SampleClasses.classWithFieldOfType(folder, SAMPLES, "GoodSample", BASE + ".web.CorrelationIdWebFilter");
        GatewayArchitectureRules.noDependencyOnOtherModules(BASE).check(classes);
        GatewayArchitectureRules.noPersistenceKafkaOrJdbc(BASE).check(classes);
        GatewayArchitectureRules.noLombokClasses(BASE).check(classes);
    }

    @ParameterizedTest(name = "rejects {0}")
    @ValueSource(strings = {
            "jakarta.persistence.EntityManager",
            "org.springframework.data.repository.Repository",
            "org.springframework.kafka.core.KafkaTemplate",
            "org.apache.kafka.clients.producer.KafkaProducer",
            "java.sql.Connection",
            "javax.sql.DataSource"})
    @DisplayName("AC-22: the technology rule rejects JPA, Spring Data, Kafka and JDBC classes")
    void rejects_persistence_kafka_and_jdbc(String forbiddenType) {
        String simpleName = forbiddenType.substring(forbiddenType.lastIndexOf('.') + 1);
        assertRejects(GatewayArchitectureRules.noPersistenceKafkaOrJdbc(BASE), "BadUses" + simpleName, forbiddenType);
    }

    @Test
    @DisplayName("AC-22: the technology rule covers exactly the packages that the spec names")
    void technology_list_is_what_the_spec_says() {
        assertThat(GatewayArchitectureRules.FORBIDDEN_TECHNOLOGY).containsExactlyInAnyOrder(
                "jakarta.persistence..", "org.springframework.data..", "org.springframework.kafka..",
                "org.apache.kafka..", "java.sql..", "javax.sql..");
    }

    @Test
    @DisplayName("AC-22: the Lombok rule rejects a class that uses a Lombok class")
    void rejects_lombok_class() {
        assertRejects(GatewayArchitectureRules.noLombokClasses(BASE), "BadUsesLombok", "lombok.val");
    }

    @Test
    @DisplayName("AC-22: the source check finds Lombok in a Java file, and finds nothing in a clean file")
    void source_check_can_fail() throws IOException {
        Files.writeString(folder.resolve("Bad.java"), "package x;\nimport lombok.Getter;\n@Getter class Bad { }\n");
        Files.writeString(folder.resolve("Good.java"), "package x;\nclass Good { }\n");

        assertThat(NoLombok.sourceViolations(folder)).hasSize(1).first().asString().contains("Bad.java:2");
    }

    @Test
    @DisplayName("AC-22: the POM check finds the Lombok dependency, and the config check finds lombok.config")
    void pom_and_config_checks_can_fail() throws IOException {
        assertThat(NoLombok.pomUsesLombok("<dependency><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId></dependency>")).isTrue();
        assertThat(NoLombok.pomUsesLombok("<dependency><artifactId>spring-boot-starter-security</artifactId></dependency>")).isFalse();

        assertThat(NoLombok.hasLombokConfig(folder)).isFalse();
        Files.writeString(folder.resolve("lombok.config"), "config.stopBubbling = true\n");
        assertThat(NoLombok.hasLombokConfig(folder)).isTrue();
    }
}
