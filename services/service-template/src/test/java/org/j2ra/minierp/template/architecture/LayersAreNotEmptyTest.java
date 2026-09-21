package org.j2ra.minierp.template.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A rule on an empty set of classes passes without checking anything.
 * This test makes sure every layer that a rule looks at has at least one class in the template.
 * (in.kafka and out.messaging stay empty on purpose: no Kafka in the template.)
 */
class LayersAreNotEmptyTest {

    private static final JavaClasses CLASSES =
            new ClassFileImporter().withImportOption(new ImportOption.DoNotIncludeTests())
                    .importPackages(ArchitectureTest.BASE);

    @Test
    void every_layer_used_by_a_rule_has_classes() {
        for (String layer : new String[] {
                ".domain", ".application", ".application.port.in", ".application.port.out",
                ".infrastructure.in.rest", ".infrastructure.out.persistence", ".infrastructure.config"}) {
            assertThat(CLASSES.stream().filter(c -> c.getPackageName().equals(ArchitectureTest.BASE + layer)
                            && !c.getSimpleName().equals("package-info")))
                    .as("classes in %s", layer)
                    .isNotEmpty();
        }
    }
}
