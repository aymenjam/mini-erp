package org.j2ra.minierp.customers.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AC-6: every rule must be able to fail. Each rule runs on wrong sample classes
 * (src/test/java/org/j2ra/minierp/archfixture/rN). The rule must reject them and name each bad class.
 * Without this test a rule that never fails would look the same as a rule that works.
 */
class ArchitectureRulesCanFailTest {

    private static final String FIXTURES = "org.j2ra.minierp.archfixture.";

    private static void assertRejects(String fixtureCase, Function<String, ArchRule> rule, String... badClasses) {
        String base = FIXTURES + fixtureCase;
        JavaClasses classes = new ClassFileImporter().importPackages(base);
        assertThat(classes).as("sample classes of %s exist", fixtureCase).isNotEmpty();
        assertThatThrownBy(() -> rule.apply(base).check(classes))
                .isInstanceOf(AssertionError.class)
                .satisfies(error -> {
                    for (String badClass : badClasses) {
                        assertThat(error.getMessage()).as("violation names %s", badClass).contains(badClass);
                    }
                });
    }

    @Test
    @DisplayName("AC-6 / AC-2: R1 rejects Spring, JPA, Kafka and infrastructure in the domain")
    void r1_domain_is_pure_java() {
        assertRejects("r1", ArchitectureRules::domainIsPureJava,
                "BadDomainSpring", "BadDomainJpa", "BadDomainKafka", "BadDomainInfrastructure");
    }

    @Test
    @DisplayName("AC-6 / AC-3: R2 rejects infrastructure and Spring stereotypes in the application layer")
    void r2_application_depends_on_domain_only() {
        assertRejects("r2", ArchitectureRules::applicationDependsOnDomainOnly,
                "BadUseCaseUsesInfrastructure", "BadUseCaseUsesSpring");
    }

    @Test
    @DisplayName("AC-6 / AC-4: R3 rejects an inbound adapter that uses an output port, a use case class or an outbound adapter")
    void r3_inbound_adapters_use_input_ports_only() {
        assertRejects("r3", ArchitectureRules::inboundAdaptersUseInputPortsOnly,
                "SomeOutPort", "SomeUseCaseClass", "SomeAdapter");
    }

    @Test
    @DisplayName("AC-6: R4 rejects an outbound adapter that uses an input port, a use case class or an inbound adapter")
    void r4_outbound_adapters_use_output_ports_only() {
        assertRejects("r4", ArchitectureRules::outboundAdaptersUseOutputPortsOnly,
                "SomeInPort", "SomeUseCaseClass", "SomeController");
    }

    @Test
    @DisplayName("AC-6: R5 rejects an adapter that depends on another adapter")
    void r5_adapters_are_independent() {
        assertRejects("r5", ArchitectureRules::adaptersAreIndependent, "BadListener");
    }

    @Test
    @DisplayName("AC-6 / AC-5: R6 rejects @Transactional outside the application layer (class, method, jakarta)")
    void r6_transactional_only_in_application() {
        assertRejects("r6", ArchitectureRules::transactionalOnlyInApplication,
                "BadTxClass", "BadTxMethod", "BadJakartaTx");
    }

    @Test
    @DisplayName("AC-6 / AC-5: R7 rejects JPA annotations outside the persistence adapter")
    void r7_jpa_only_in_persistence_adapter() {
        assertRejects("r7", ArchitectureRules::jpaOnlyInPersistenceAdapter,
                "BadEntity", "BadMappedSuperclass", "BadEmbeddable");
    }

    @Test
    @DisplayName("AC-6: R8a rejects a controller outside the REST adapter")
    void r8a_controllers_only_in_rest_adapter() {
        assertRejects("r8", ArchitectureRules::controllersOnlyInRestAdapter, "BadController");
    }

    @Test
    @DisplayName("AC-6: R8b rejects a Kafka listener outside the Kafka adapter")
    void r8b_kafka_listeners_only_in_kafka_adapter() {
        assertRejects("r8", ArchitectureRules::kafkaListenersOnlyInKafkaAdapter, "BadKafkaListener");
    }

    @Test
    @DisplayName("AC-6: R9 rejects a dependency on infrastructure.config")
    void r9_nothing_depends_on_config() {
        assertRejects("r9", ArchitectureRules::nothingDependsOnConfig, "BadAdapter");
    }

    @Test
    @DisplayName("AC-6 / AC-21: R10 rejects a GraphQL adapter that uses an output port, a use case class or an outbound adapter")
    void r10_graphql_adapters_use_input_ports_only() {
        assertRejects("r10", ArchitectureRules::graphqlAdaptersUseInputPortsOnly,
                "SomeOutPort", "SomeUseCaseClass", "SomeAdapter");
    }

    @Test
    @DisplayName("AC-6 / AC-23: R11 rejects each endpoint method without @Operation, and accepts a documented one")
    void r11_endpoints_have_operation_annotation() {
        assertRejects("r11", ArchitectureRules::endpointsHaveOperationAnnotation,
                "BadController.get", "BadController.post", "BadController.put", "BadController.patch", "BadController.delete");
        String base = FIXTURES + "r11";
        assertThatThrownBy(() -> ArchitectureRules.endpointsHaveOperationAnnotation(base)
                .check(new ClassFileImporter().importPackages(base)))
                .as("the documented endpoint is not reported")
                .satisfies(error -> assertThat(error.getMessage()).doesNotContain("GoodController"));
    }

    @Test
    @DisplayName("AC-6 / AC-18: L4 rejects Lombok-generated code in the domain and in the application layer (bytecode)")
    void l4_rejects_generated_code_outside_infrastructure() {
        assertRejects("l1", ArchitectureRules::generatedCodeOnlyInInfrastructure, "BadDomainWithGetter");
        assertRejects("l2", ArchitectureRules::generatedCodeOnlyInInfrastructure, "BadApplicationWithSlf4j");
    }

    // --- Lombok rules (source scan, see LombokRules) ---

    private static void assertLombokRejects(String fixtureCase, java.util.function.Function<java.nio.file.Path, java.util.List<String>> rule,
                                            String... badClasses) {
        java.nio.file.Path root = LombokUsageTest.moduleDir().resolve("src/test/java/org/j2ra/minierp/archfixture/" + fixtureCase);
        assertThat(LombokRules.countJavaFiles(root)).as("sample classes of %s exist", fixtureCase).isPositive();
        String violations = String.join("\n", rule.apply(root));
        for (String badClass : badClasses) {
            assertThat(violations).as("violation names %s", badClass).contains(badClass);
        }
    }

    @Test
    @DisplayName("AC-6 / AC-18: L1 rejects Lombok in the domain")
    void l1_rejects_lombok_in_domain() {
        assertLombokRejects("l1", LombokRules::notInDomain, "BadDomainWithGetter");
    }

    @Test
    @DisplayName("AC-6 / AC-18: L2 rejects Lombok outside infrastructure (application)")
    void l2_rejects_lombok_in_application() {
        assertLombokRejects("l2", LombokRules::onlyInInfrastructure, "BadApplicationWithSlf4j");
    }

    @Test
    @DisplayName("AC-6 / AC-18: L3 rejects @Data, @ToString, @EqualsAndHashCode (on JPA entities) and wildcard imports")
    void l3_rejects_forbidden_features() {
        assertLombokRejects("l3", LombokRules::onlyAllowedFeatures,
                "BadEntityWithData", "BadEntityWithToString", "BadEntityWithEqualsAndHashCode", "BadWildcardImport");
    }

    @Test
    @DisplayName("AC-6 / AC-18: L5 rejects a public @NoArgsConstructor, other arguments and a wrong package, but accepts the protected form")
    void l5_rejects_wrong_no_args_constructor() {
        assertLombokRejects("l5", LombokRules::noArgsConstructorOnlyProtectedInPersistence,
                "BadPublicNoArgs", "BadExplicitPublicNoArgs", "BadNoArgsWithForce", "BadNoArgsOutsidePersistence");
        String violations = String.join("\n", LombokRules.noArgsConstructorOnlyProtectedInPersistence(
                LombokUsageTest.moduleDir().resolve("src/test/java/org/j2ra/minierp/archfixture/l5")));
        assertThat(violations).as("the allowed form is not reported").doesNotContain("GoodProtectedEntity");
    }
}
