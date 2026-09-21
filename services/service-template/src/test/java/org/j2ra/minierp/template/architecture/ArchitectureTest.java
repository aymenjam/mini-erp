package org.j2ra.minierp.template.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Architecture rules on the real service (AC-2 to AC-5). A failing rule blocks the build.
 * Test classes are not imported: the wrong samples of ArchitectureRulesCanFailTest must not hit these rules.
 * In a copy of the template, change BASE to the new base package.
 */
@AnalyzeClasses(packages = ArchitectureTest.BASE, importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    static final String BASE = "org.j2ra.minierp.template";

    @ArchTest
    static final ArchRule ac2_domain_is_pure_java = ArchitectureRules.domainIsPureJava(BASE);

    @ArchTest
    static final ArchRule ac3_application_depends_on_domain_only = ArchitectureRules.applicationDependsOnDomainOnly(BASE);

    @ArchTest
    static final ArchRule ac4_inbound_adapters_use_input_ports_only = ArchitectureRules.inboundAdaptersUseInputPortsOnly(BASE);

    @ArchTest
    static final ArchRule outbound_adapters_use_output_ports_only = ArchitectureRules.outboundAdaptersUseOutputPortsOnly(BASE);

    @ArchTest
    static final ArchRule adapters_are_independent = ArchitectureRules.adaptersAreIndependent(BASE);

    @ArchTest
    static final ArchRule ac5_transactional_only_in_application = ArchitectureRules.transactionalOnlyInApplication(BASE);

    @ArchTest
    static final ArchRule ac5_jpa_only_in_persistence_adapter = ArchitectureRules.jpaOnlyInPersistenceAdapter(BASE);

    @ArchTest
    static final ArchRule controllers_only_in_rest_adapter = ArchitectureRules.controllersOnlyInRestAdapter(BASE);

    @ArchTest
    static final ArchRule kafka_listeners_only_in_kafka_adapter = ArchitectureRules.kafkaListenersOnlyInKafkaAdapter(BASE);

    @ArchTest
    static final ArchRule graphql_adapters_use_input_ports_only = ArchitectureRules.graphqlAdaptersUseInputPortsOnly(BASE);

    @ArchTest
    static final ArchRule endpoints_have_operation_annotation = ArchitectureRules.endpointsHaveOperationAnnotation(BASE);

    @ArchTest
    static final ArchRule nothing_depends_on_config = ArchitectureRules.nothingDependsOnConfig(BASE);

    @ArchTest
    static final ArchRule l4_no_lombok_generated_code_outside_infrastructure = ArchitectureRules.generatedCodeOnlyInInfrastructure(BASE);
}
