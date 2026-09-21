package org.j2ra.minierp.gateway.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** AC-22: the small architecture rule set of the gateway, on the real classes. A failing rule blocks the build. */
class GatewayArchitectureTest {

    static final String BASE = "org.j2ra.minierp.gateway";

    private static final JavaClasses GATEWAY = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages(BASE);

    static Path moduleDir() {
        // Surefire sets basedir to the module folder.
        return Path.of(System.getProperty("basedir", ".")).toAbsolutePath().normalize();
    }

    @Test
    @DisplayName("AC-22: the rules run on real classes (an empty import would make every rule pass)")
    void gateway_classes_are_found() {
        assertThat(GATEWAY.stream().map(javaClass -> javaClass.getName()))
                .contains("org.j2ra.minierp.gateway.GatewayApplication", "org.j2ra.minierp.gateway.web.CorrelationIdWebFilter");
    }

    @Test
    @DisplayName("AC-22: no gateway class depends on a package of a service or of another module")
    void no_dependency_on_other_modules() {
        GatewayArchitectureRules.noDependencyOnOtherModules(BASE).check(GATEWAY);
    }

    @Test
    @DisplayName("AC-22: no gateway class uses JPA, Spring Data, Kafka or JDBC")
    void no_persistence_kafka_or_jdbc() {
        GatewayArchitectureRules.noPersistenceKafkaOrJdbc(BASE).check(GATEWAY);
    }

    @Test
    @DisplayName("AC-22: no gateway class uses Lombok")
    void no_lombok_classes() {
        GatewayArchitectureRules.noLombokClasses(BASE).check(GATEWAY);
    }

    @Test
    @DisplayName("AC-22: no Lombok in the source, in the POM, and no lombok.config in the gateway")
    void no_lombok_in_source_pom_or_config() throws IOException {
        assertThat(NoLombok.sourceViolations(moduleDir().resolve("src/main/java"))).isEmpty();
        assertThat(NoLombok.pomUsesLombok(Files.readString(moduleDir().resolve("pom.xml")))).isFalse();
        assertThat(NoLombok.hasLombokConfig(moduleDir())).isFalse();
    }
}
