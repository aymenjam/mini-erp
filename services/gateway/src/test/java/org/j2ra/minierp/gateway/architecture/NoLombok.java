package org.j2ra.minierp.gateway.architecture;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * "No Lombok in the gateway" (spec 0.4). Most Lombok annotations are not in the bytecode, so ArchUnit cannot see them.
 * We look at the three places where Lombok would show: the Java source, the POM, and a lombok.config file.
 */
final class NoLombok {

    private static final Pattern LOMBOK_IN_SOURCE = Pattern.compile("\\blombok\\b");

    private NoLombok() {
    }

    static List<String> sourceViolations(Path javaRoot) {
        try (Stream<Path> files = Files.walk(javaRoot)) {
            return files.filter(path -> path.toString().endsWith(".java"))
                    .flatMap(path -> linesWithLombok(path).stream())
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static boolean pomUsesLombok(String pomContent) {
        return pomContent.contains("<artifactId>lombok</artifactId>") || pomContent.contains("org.projectlombok");
    }

    static boolean hasLombokConfig(Path moduleDir) {
        return Files.exists(moduleDir.resolve("lombok.config"));
    }

    private static List<String> linesWithLombok(Path file) {
        try {
            List<String> lines = Files.readAllLines(file);
            return java.util.stream.IntStream.range(0, lines.size())
                    .filter(i -> LOMBOK_IN_SOURCE.matcher(lines.get(i)).find())
                    .mapToObj(i -> file.getFileName() + ":" + (i + 1) + " " + lines.get(i).strip())
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
