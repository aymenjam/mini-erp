package org.j2ra.minierp.customers.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.j2ra.minierp.customers.application.port.in.ListCustomersUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** AC-9: the controller calls the input port ListCustomersUseCase and no other class of the application layer. */
class CustomerControllerDependenciesTest {

    @Test
    @DisplayName("AC-9: CustomerController depends on ListCustomersUseCase only, in the application layer")
    void controller_uses_only_the_list_customers_port() {
        JavaClass controller = new ClassFileImporter().withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages(ArchitectureTest.BASE)
                .get(ArchitectureTest.BASE + ".infrastructure.in.rest.CustomerController");

        Set<String> applicationDependencies = controller.getDirectDependenciesFromSelf().stream()
                .map(dependency -> dependency.getTargetClass().getName())
                .filter(name -> name.startsWith(ArchitectureTest.BASE + ".application"))
                .collect(Collectors.toSet());

        assertThat(applicationDependencies).containsExactly(ListCustomersUseCase.class.getName());
    }
}
