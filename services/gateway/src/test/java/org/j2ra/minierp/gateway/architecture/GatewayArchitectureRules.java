package org.j2ra.minierp.gateway.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchRule;

import java.util.List;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * The small rule set of the gateway (AC-22, constitution.md section 5). The gateway is not hexagonal, so the layer rules of the services do not apply.
 * Each rule takes the base package as a parameter, so the tests can run it on wrong sample classes (see GatewayArchitectureRulesCanFailTest).
 */
final class GatewayArchitectureRules {

    /** Every package of every service and of the shared code starts with this. */
    static final String PROJECT = "org.j2ra.minierp";

    /** Technology that belongs to the services, not to a gateway: persistence, Kafka, JDBC. */
    static final List<String> FORBIDDEN_TECHNOLOGY = List.of(
            "jakarta.persistence..", "org.springframework.data..", "org.springframework.kafka..",
            "org.apache.kafka..", "java.sql..", "javax.sql..");

    private GatewayArchitectureRules() {
    }

    /** A gateway that knows the classes of a service is coupled to it. Services are reached over HTTP only. */
    static ArchRule noDependencyOnOtherModules(String base) {
        DescribedPredicate<JavaClass> outsideTheGateway = resideInAPackage(PROJECT + "..")
                .and(DescribedPredicate.not(resideInAPackage(base + "..")));
        return noClasses().that().resideInAPackage(base + "..")
                .should().dependOnClassesThat(outsideTheGateway)
                .as("the gateway does not depend on any other package of " + PROJECT);
    }

    static ArchRule noPersistenceKafkaOrJdbc(String base) {
        return noClasses().that().resideInAPackage(base + "..")
                .should().dependOnClassesThat().resideInAnyPackage(FORBIDDEN_TECHNOLOGY.toArray(String[]::new))
                .as("the gateway uses no JPA, Spring Data, Kafka or JDBC class");
    }

    /** The part of "no Lombok" that the bytecode can show. The source and the POM are checked by NoLombok. */
    static ArchRule noLombokClasses(String base) {
        return noClasses().that().resideInAPackage(base + "..")
                .should().dependOnClassesThat().resideInAPackage("lombok..")
                .as("the gateway uses no Lombok class");
    }
}
