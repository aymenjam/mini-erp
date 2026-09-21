package org.j2ra.minierp.template.architecture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Lombok rules on the real service sources (AGENTS.md, "Lombok"). A failing rule blocks the build. */
class LombokUsageTest {

    /** The module folder: target/test-classes -> target -> module. Works from Maven and from the IDE. */
    static Path moduleDir() {
        try {
            Path testClasses = Path.of(LombokUsageTest.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            return testClasses.getParent().getParent();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private static final Path MAIN_SOURCES = moduleDir().resolve("src/main/java");

    @Test
    @DisplayName("Rules are not run on an empty folder")
    void sources_are_found() {
        assertThat(LombokRules.countJavaFiles(MAIN_SOURCES)).isGreaterThan(10);
    }

    @Test
    @DisplayName("AC-18 / L1: no Lombok in the domain")
    void l1_no_lombok_in_domain() {
        assertThat(LombokRules.notInDomain(MAIN_SOURCES)).isEmpty();
    }

    @Test
    @DisplayName("AC-18 / L2: Lombok only in infrastructure")
    void l2_lombok_only_in_infrastructure() {
        assertThat(LombokRules.onlyInInfrastructure(MAIN_SOURCES)).isEmpty();
    }

    @Test
    @DisplayName("AC-18 / L3: only @Getter, @RequiredArgsConstructor, @Slf4j and @NoArgsConstructor")
    void l3_only_allowed_features() {
        List<String> violations = LombokRules.onlyAllowedFeatures(MAIN_SOURCES);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("AC-18 / L5: @NoArgsConstructor only as (access = AccessLevel.PROTECTED), only in infrastructure.out.persistence")
    void l5_no_args_constructor_only_protected_in_persistence() {
        assertThat(LombokRules.noArgsConstructorOnlyProtectedInPersistence(MAIN_SOURCES)).isEmpty();
    }
}
