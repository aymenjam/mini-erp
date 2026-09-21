package org.j2ra.minierp.template.architecture;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Lombok rules of AGENTS.md. They read the Java SOURCE files, not the bytecode: @Getter, @Slf4j and the others are
 * removed by the compiler, so ArchUnit cannot see them. (lombok.config bans the other features at compile time.)
 * Each rule takes the source folder as a parameter, so it also runs on the wrong samples in archfixture.
 */
final class LombokRules {

    /**
     * The only Lombok types we use. AccessLevel is the helper type of @NoArgsConstructor(access = AccessLevel.PROTECTED).
     * A full name such as lombok.AccessLevel.PROTECTED counts as its type.
     */
    static final Set<String> ALLOWED = Set.of("lombok.Getter", "lombok.RequiredArgsConstructor", "lombok.extern.slf4j.Slf4j",
            "lombok.NoArgsConstructor", "lombok.AccessLevel");

    private static final String PERSISTENCE_ADAPTER = "infrastructure/out/persistence";

    /** @NoArgsConstructor with its arguments, if any. Group 1 = the text between the brackets. */
    private static final Pattern NO_ARGS_CONSTRUCTOR =
            Pattern.compile("@(?:lombok\\.)?NoArgsConstructor\\b\\s*(?:\\(([^)]*)\\))?");
    private static final Pattern PROTECTED_ONLY = Pattern.compile("access=(?:lombok\\.)?AccessLevel\\.PROTECTED");

    private static final Pattern LOMBOK_TYPE = Pattern.compile("\\blombok\\.[A-Za-z0-9_.]*[A-Za-z0-9_*]");
    private static final Pattern COMMENTS = Pattern.compile("//[^\\n]*|/\\*.*?\\*/", Pattern.DOTALL);

    private LombokRules() {
    }

    /** L1: no Lombok in the domain (records and explicit methods). */
    static List<String> notInDomain(Path sourceRoot) {
        return violations(sourceRoot, "L1 no Lombok in domain", (relative, types) ->
                inLayer(relative, "domain") && !types.isEmpty());
    }

    /** L2: Lombok only in infrastructure (so not in application, not in the base package). */
    static List<String> onlyInInfrastructure(Path sourceRoot) {
        return violations(sourceRoot, "L2 Lombok only in infrastructure", (relative, types) ->
                !inLayer(relative, "infrastructure") && !types.isEmpty());
    }

    /**
     * L3: only @Getter, @RequiredArgsConstructor, @Slf4j and @NoArgsConstructor (its form is checked by L5).
     * No @Data, @ToString, @EqualsAndHashCode (also not on JPA entities), no @AllArgsConstructor, no wildcard import.
     */
    static List<String> onlyAllowedFeatures(Path sourceRoot) {
        return violations(sourceRoot, "L3 only @Getter, @RequiredArgsConstructor, @Slf4j, @NoArgsConstructor", (relative, types) ->
                types.stream().anyMatch(type -> ALLOWED.stream().noneMatch(a -> type.equals(a) || type.startsWith(a + "."))));
    }

    /**
     * L5: @NoArgsConstructor only as @NoArgsConstructor(access = AccessLevel.PROTECTED) and only in infrastructure.out.persistence.
     * JPA needs a protected no-arg constructor, and constructors are not inherited. lombok.config cannot check the access
     * level, so this test does. Nothing else may be inside the brackets (no force, no other access level).
     */
    static List<String> noArgsConstructorOnlyProtectedInPersistence(Path sourceRoot) {
        List<String> result = new ArrayList<>();
        javaFiles(sourceRoot).forEach(file -> {
            String relative = sourceRoot.relativize(file).toString().replace('\\', '/');
            Matcher matcher = NO_ARGS_CONSTRUCTOR.matcher(strippedCode(file));
            while (matcher.find()) {
                String arguments = matcher.group(1) == null ? "" : matcher.group(1).replaceAll("\\s+", "");
                boolean rightPlace = ("/" + relative).contains("/" + PERSISTENCE_ADAPTER + "/");
                if (!rightPlace || !PROTECTED_ONLY.matcher(arguments).matches()) {
                    result.add("L5 @NoArgsConstructor only as (access = AccessLevel.PROTECTED) in infrastructure.out.persistence: "
                            + relative + " has " + matcher.group().replaceAll("\\s+", " "));
                }
            }
        });
        return result;
    }

    static long countJavaFiles(Path sourceRoot) {
        return javaFiles(sourceRoot).count();
    }

    private interface Check {
        boolean isViolation(String relativePath, List<String> lombokTypes);
    }

    private static List<String> violations(Path sourceRoot, String rule, Check check) {
        List<String> result = new ArrayList<>();
        javaFiles(sourceRoot).forEach(file -> {
            String relative = sourceRoot.relativize(file).toString().replace('\\', '/');
            List<String> types = lombokTypes(file);
            if (check.isViolation(relative, types)) {
                result.add(rule + ": " + relative + " uses " + types);
            }
        });
        return result;
    }

    private static boolean inLayer(String relativePath, String layer) {
        return ("/" + relativePath).contains("/" + layer + "/");
    }

    private static String strippedCode(Path file) {
        try {
            return COMMENTS.matcher(Files.readString(file)).replaceAll(" ");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<String> lombokTypes(Path file) {
        List<String> types = new ArrayList<>();
        Matcher matcher = LOMBOK_TYPE.matcher(strippedCode(file));
        while (matcher.find()) {
            types.add(matcher.group());
        }
        return types.stream().distinct().toList();
    }

    private static Stream<Path> javaFiles(Path sourceRoot) {
        try (Stream<Path> files = Files.walk(sourceRoot)) {
            return files.filter(path -> path.toString().endsWith(".java")).toList().stream();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
