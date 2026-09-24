package org.j2ra.minierp.customers.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AC-9: a controller calls input ports (use case interfaces, and the commands nested in them) and no other class of the application layer.
 * The check runs on every controller of the REST adapter.
 */
class CustomerControllerDependenciesTest {

    private static final String PORT_IN = ArchitectureTest.BASE + ".application.port.in.";
    private static final List<String> CONTROLLERS = List.of("CustomerController", "CustomerContactController", "CustomerAddressController");

    @Test
    @DisplayName("AC-9: every customer controller depends on the input ports only, in the application layer")
    void controllers_use_only_input_ports() {
        JavaClasses classes = new ClassFileImporter().withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages(ArchitectureTest.BASE);

        for (String name : CONTROLLERS) {
            JavaClass controller = classes.get(ArchitectureTest.BASE + ".infrastructure.in.rest." + name);

            Set<String> applicationDependencies = controller.getDirectDependenciesFromSelf().stream()
                    .map(dependency -> dependency.getTargetClass().getName())
                    .filter(target -> target.startsWith(ArchitectureTest.BASE + ".application"))
                    .collect(Collectors.toSet());

            assertThat(applicationDependencies).as(name).isNotEmpty().allMatch(target -> target.startsWith(PORT_IN));
        }
    }
}
