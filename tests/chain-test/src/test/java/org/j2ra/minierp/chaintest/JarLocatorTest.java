package org.j2ra.minierp.chaintest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * How the chain test finds the two jars. An IDE may not put the jars on the classpath (it often uses target/classes of the module),
 * so the fallback and the message are tested here, with a class loader that has no manifest of the applications.
 */
class JarLocatorTest {

    private static final String START_CLASS = "org.example.SomeApplication";

    @TempDir
    Path folder;

    @Test
    @DisplayName("Chain test: under Maven, both jars are found on the classpath")
    void jars_are_found_from_the_real_classpath() {
        assertThat(JarLocator.gateway()).exists().hasFileName("gateway.jar");
        assertThat(JarLocator.customers()).exists().hasFileName("customers.jar");
    }

    @Test
    @DisplayName("Chain test: a jar whose manifest has the Start-Class is found on the classpath")
    void jar_is_found_by_its_manifest() throws IOException {
        Path jar = folder.resolve("some-name.jar");
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().putValue("Start-Class", START_CLASS);
        try (OutputStream out = Files.newOutputStream(jar); JarOutputStream jarOut = new JarOutputStream(out, manifest)) {
            jarOut.putNextEntry(new ZipEntry("empty.txt"));
            jarOut.closeEntry();
        }

        try (URLClassLoader loader = new URLClassLoader(new URL[]{jar.toUri().toURL()}, null)) {
            assertThat(JarLocator.find("some", START_CLASS, loader, folder)).isEqualTo(jar);
        }
    }

    @Test
    @DisplayName("Chain test: without a jar on the classpath (IDE case), the jar in services/<name>/target is found from a parent folder")
    void fallback_finds_the_jar_in_the_target_folder() throws IOException {
        Path jar = folder.resolve("services/some/target/some.jar");
        Files.createDirectories(jar.getParent());
        Files.writeString(jar, "not a real jar, only the file matters here");
        Path startDir = Files.createDirectories(folder.resolve("tests/chain-test"));

        try (URLClassLoader noApplications = new URLClassLoader(new URL[0], null)) {
            assertThat(JarLocator.find("some", START_CLASS, noApplications, startDir)).isEqualTo(jar);
        }
    }

    @Test
    @DisplayName("Chain test: a missing jar gives a clear message with the command to run")
    void missing_jar_gives_a_clear_message() throws IOException {
        try (URLClassLoader noApplications = new URLClassLoader(new URL[0], null)) {
            assertThatThrownBy(() -> JarLocator.find("some", START_CLASS, noApplications, folder))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("some.jar was not found")
                    .hasMessageContaining("services/some/target/some.jar")
                    .hasMessageContaining("./mvnw -pl tests/chain-test -am verify");
        }
    }
}
