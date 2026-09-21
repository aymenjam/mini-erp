package org.j2ra.minierp.gateway.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.springframework.asm.ClassWriter;
import org.springframework.asm.Opcodes;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Makes wrong sample classes for the rule tests: a class with one field of the forbidden type.
 * The gateway cannot compile a real sample (it has no JPA or Kafka library, and it must not get one), but ArchUnit only reads
 * the bytecode. A field type is enough for it to see the dependency. Spring's own copy of ASM writes the bytecode (no new library).
 */
final class SampleClasses {

    private SampleClasses() {
    }

    /** Writes {@code <packageName>.<className>} with a field of type {@code fieldType} into the folder, and imports it. */
    static JavaClasses classWithFieldOfType(Path folder, String packageName, String className, String fieldType) {
        String internalName = packageName.replace('.', '/') + "/" + className;
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, internalName, null, "java/lang/Object", null);
        writer.visitField(Opcodes.ACC_PRIVATE, "forbidden", "L" + fieldType.replace('.', '/') + ";", null, null).visitEnd();
        writer.visitEnd();
        try {
            Path file = folder.resolve(internalName + ".class");
            Files.createDirectories(file.getParent());
            Files.write(file, writer.toByteArray());
            return new ClassFileImporter().importPath(folder);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
