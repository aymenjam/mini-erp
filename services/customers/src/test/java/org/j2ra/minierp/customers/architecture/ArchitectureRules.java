package org.j2ra.minierp.customers.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.CompositeArchRule;

import java.util.stream.Stream;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noConstructors;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * The hexagonal rules of .claude/rules/hexagonal-architecture.md and ADR-006. Each rule is a function of the base package,
 * so the same rule runs on the real service and on the wrong sample classes in ArchitectureRulesCanFailTest.
 * Annotations and Kafka types are matched by name: the template has no Kafka jar.
 */
final class ArchitectureRules {

    private static final String SPRING_TX = "org.springframework.transaction.annotation.Transactional";
    private static final String JAKARTA_TX = "jakarta.transaction.Transactional";

    private ArchitectureRules() {
    }

    /** R1 (AC-2): the domain is pure Java. It may use only java.* and itself. */
    static ArchRule domainIsPureJava(String base) {
        return classes().that().resideInAPackage(base + ".domain..")
                .should().onlyDependOnClassesThat().resideInAnyPackage("java..", base + ".domain..")
                .as("R1: domain depends only on java.* and the domain (no Spring, JPA, Kafka, application, infrastructure)");
    }

    /** R2 (AC-3): the application layer knows the domain and the transaction annotation, nothing else. */
    static ArchRule applicationDependsOnDomainOnly(String base) {
        return classes().that().resideInAPackage(base + ".application..")
                .should().onlyDependOnClassesThat().resideInAnyPackage(
                        "java..", base + ".application..", base + ".domain..",
                        "org.springframework.transaction.annotation..")
                .as("R2: application depends only on domain, itself, java.* and Spring transaction annotations");
    }

    /** R3 (AC-4): inbound adapters call input ports. They do not know output ports, use case classes or outbound adapters. */
    static ArchRule inboundAdaptersUseInputPortsOnly(String base) {
        DescribedPredicate<JavaClass> forbidden =
                resideInAPackage(base + ".application..").and(not(resideInAPackage(base + ".application.port.in..")))
                        .or(resideInAPackage(base + ".infrastructure.out.."));
        return noClasses().that().resideInAPackage(base + ".infrastructure.in..")
                .should().dependOnClassesThat(forbidden)
                .as("R3: infrastructure.in uses application.port.in and domain only (not port.out, use case classes, out.*)");
    }

    /** R4: outbound adapters implement output ports. They do not know input ports, use case classes or inbound adapters. */
    static ArchRule outboundAdaptersUseOutputPortsOnly(String base) {
        DescribedPredicate<JavaClass> forbidden =
                resideInAPackage(base + ".application..").and(not(resideInAPackage(base + ".application.port.out..")))
                        .or(resideInAPackage(base + ".infrastructure.in.."));
        return noClasses().that().resideInAPackage(base + ".infrastructure.out..")
                .should().dependOnClassesThat(forbidden)
                .as("R4: infrastructure.out uses application.port.out and domain only (not port.in, use case classes, in.*)");
    }

    /** R5: adapters (rest, kafka, persistence, messaging) do not depend on each other. Config is not an adapter. */
    static ArchRule adaptersAreIndependent(String base) {
        return slices().matching(base + ".infrastructure.(*).(*)..")
                .should().notDependOnEachOther()
                .as("R5: adapters (in.rest, in.kafka, out.persistence, out.messaging) do not depend on each other");
    }

    /** R6 (AC-5): transactions start in the application layer only (class or method level). */
    static ArchRule transactionalOnlyInApplication(String base) {
        ArchRule onClasses = noClasses().that().resideOutsideOfPackage(base + ".application..")
                .should().beAnnotatedWith(SPRING_TX)
                .orShould().beAnnotatedWith(JAKARTA_TX);
        ArchRule onMethods = noMethods().that().areDeclaredInClassesThat().resideOutsideOfPackage(base + ".application..")
                .should().beAnnotatedWith(SPRING_TX)
                .orShould().beAnnotatedWith(JAKARTA_TX);
        return CompositeArchRule.of(onClasses).and(onMethods)
                .as("R6: @Transactional is used only in the application layer");
    }

    /** R7 (AC-5): JPA mapping annotations only in the persistence adapter. Domain objects stay plain. */
    static ArchRule jpaOnlyInPersistenceAdapter(String base) {
        return noClasses().that().resideOutsideOfPackage(base + ".infrastructure.out.persistence..")
                .should().beAnnotatedWith("jakarta.persistence.Entity")
                .orShould().beAnnotatedWith("jakarta.persistence.MappedSuperclass")
                .orShould().beAnnotatedWith("jakarta.persistence.Embeddable")
                .as("R7: @Entity, @MappedSuperclass and @Embeddable only in infrastructure.out.persistence");
    }

    /**
     * R8a: controllers live in the REST adapter.
     * The template has no controller in main (no endpoints), so the set is empty on purpose.
     * The sample class in ArchitectureRulesCanFailTest proves that the rule can fail.
     */
    static ArchRule controllersOnlyInRestAdapter(String base) {
        return classes().that().areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                .or().areAnnotatedWith("org.springframework.stereotype.Controller")
                .should().resideInAPackage(base + ".infrastructure.in.rest..")
                .allowEmptyShould(true)
                .as("R8a: controllers only in infrastructure.in.rest");
    }

    /** R8b: Kafka listeners live in the Kafka adapter. Empty in the template on purpose (see R8a). */
    static ArchRule kafkaListenersOnlyInKafkaAdapter(String base) {
        return methods().that().areAnnotatedWith("org.springframework.kafka.annotation.KafkaListener")
                .should().beDeclaredInClassesThat().resideInAPackage(base + ".infrastructure.in.kafka..")
                .allowEmptyShould(true)
                .as("R8b: @KafkaListener only in infrastructure.in.kafka");
    }

    /** R9: infrastructure.config is the composition root. It may see all layers, but nobody may depend on it. */
    static ArchRule nothingDependsOnConfig(String base) {
        return noClasses().that().resideOutsideOfPackage(base + ".infrastructure.config..")
                .should().dependOnClassesThat().resideInAPackage(base + ".infrastructure.config..")
                .as("R9: nothing depends on infrastructure.config");
    }

    /**
     * R10: GraphQL adapters call input ports only, like controllers. R3 already covers all of infrastructure.in; this rule
     * names GraphQL on purpose, so it stays true if R3 changes. Empty in the template on purpose (no GraphQL library, ADR-009).
     */
    static ArchRule graphqlAdaptersUseInputPortsOnly(String base) {
        DescribedPredicate<JavaClass> forbidden =
                resideInAPackage(base + ".application..").and(not(resideInAPackage(base + ".application.port.in..")))
                        .or(resideInAPackage(base + ".infrastructure.out.."));
        return noClasses().that().resideInAPackage(base + ".infrastructure.in.graphql..")
                .should().dependOnClassesThat(forbidden)
                .allowEmptyShould(true)
                .as("R10: infrastructure.in.graphql uses application.port.in and domain only (not port.out, use case classes, out.*)");
    }

    /**
     * R11: every endpoint method in the REST adapter has @Operation, so the OpenAPI document says what it does.
     * Matched by name, like the Kafka types. Empty in the template on purpose: it has no real endpoint (see R8a).
     */
    static ArchRule endpointsHaveOperationAnnotation(String base) {
        // ArchUnit reads and()/or() from left to right without precedence, so the "any mapping" part is built as one predicate.
        DescribedPredicate<JavaMethod> isEndpoint = Stream.of("Get", "Post", "Put", "Patch", "Delete")
                .map(verb -> DescribedPredicate.<JavaMethod>describe("annotated with @" + verb + "Mapping",
                        method -> method.isAnnotatedWith("org.springframework.web.bind.annotation." + verb + "Mapping")))
                .reduce((left, right) -> left.or(right)).orElseThrow();
        DescribedPredicate<JavaMethod> inRestAdapter = DescribedPredicate.describe("declared in infrastructure.in.rest",
                method -> method.getOwner().getPackageName().equals(base + ".infrastructure.in.rest")
                        || method.getOwner().getPackageName().startsWith(base + ".infrastructure.in.rest."));
        return methods().that(inRestAdapter.and(isEndpoint))
                .should().beAnnotatedWith("io.swagger.v3.oas.annotations.Operation")
                .allowEmptyShould(true)
                .as("R11: endpoint methods (@GetMapping, @PostMapping, @PutMapping, @PatchMapping, @DeleteMapping) in infrastructure.in.rest have @Operation");
    }

    /**
     * L4 (bytecode side of the Lombok rules): no Lombok-generated member outside infrastructure.
     * ArchUnit cannot see @Getter or @Slf4j (source retention), but it sees @lombok.Generated on what they generate
     * (lombok.config: addLombokGeneratedAnnotation). LombokUsageTest reads the sources; this reads the classes.
     */
    static ArchRule generatedCodeOnlyInInfrastructure(String base) {
        String outside = base + ".infrastructure..";
        // A wrong sample may have no method or no field. On the real service none of the sets is empty.
        return CompositeArchRule
                .of(noMethods().that().areDeclaredInClassesThat().resideOutsideOfPackage(outside)
                        .should().beAnnotatedWith("lombok.Generated").allowEmptyShould(true))
                .and(noConstructors().that().areDeclaredInClassesThat().resideOutsideOfPackage(outside)
                        .should().beAnnotatedWith("lombok.Generated").allowEmptyShould(true))
                .and(noFields().that().areDeclaredInClassesThat().resideOutsideOfPackage(outside)
                        .should().beAnnotatedWith("lombok.Generated").allowEmptyShould(true))
                .as("L4: no Lombok-generated code outside infrastructure");
    }
}
