package org.j2ra.minierp.customers.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.j2ra.minierp.customers.application.port.in.CreateCustomerUseCase;
import org.j2ra.minierp.customers.application.port.in.DeactivateCustomerUseCase;
import org.j2ra.minierp.customers.application.port.in.GetCustomerUseCase;
import org.j2ra.minierp.customers.application.port.in.ListCustomersUseCase;
import org.j2ra.minierp.customers.application.port.in.ReactivateCustomerUseCase;
import org.j2ra.minierp.customers.application.port.in.UpdateCustomerUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/** AC-9: the controller calls input ports (use case interfaces) and no other class of the application layer. */
class CustomerControllerDependenciesTest {

    @Test
    @DisplayName("AC-9: CustomerController depends on the input ports only, in the application layer")
    void controller_uses_only_input_ports() {
        JavaClass controller = new ClassFileImporter().withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages(ArchitectureTest.BASE)
                .get(ArchitectureTest.BASE + ".infrastructure.in.rest.CustomerController");

        Set<String> applicationDependencies = controller.getDirectDependenciesFromSelf().stream()
                .map(dependency -> dependency.getTargetClass().getName())
                .filter(name -> name.startsWith(ArchitectureTest.BASE + ".application"))
                .collect(Collectors.toSet());

        // The commands are records nested in the use case interfaces: they belong to the port.
        Set<String> allowed = Stream.of(ListCustomersUseCase.class, GetCustomerUseCase.class, CreateCustomerUseCase.class,
                        UpdateCustomerUseCase.class, DeactivateCustomerUseCase.class, ReactivateCustomerUseCase.class)
                .map(Class::getName)
                .collect(Collectors.toSet());
        assertThat(applicationDependencies)
                .allSatisfy(name -> assertThat(allowed.stream().anyMatch(port -> name.equals(port) || name.startsWith(port + "$"))).isTrue())
                .contains(ListCustomersUseCase.class.getName());
    }
}
