package org.j2ra.minierp.chaintest;

import java.nio.file.Files;
import java.nio.file.Path;

/** Finds files of the repository from any working folder, by looking in the parent folders (like ComposePostgresVersionTest does). */
final class RepoFiles {

    private RepoFiles() {
    }

    /** The module folder. Surefire sets basedir. */
    static Path moduleDir() {
        return Path.of(System.getProperty("basedir", "")).toAbsolutePath().normalize();
    }

    static Path find(String relativePath) {
        return find(relativePath, moduleDir());
    }

    static Path find(String relativePath, Path startDir) {
        for (Path dir = startDir; dir != null; dir = dir.getParent()) {
            Path candidate = dir.resolve(relativePath);
            if (Files.exists(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException(relativePath + " not found in " + startDir + " or in a parent folder");
    }

    /** The repository root: the folder that holds the given file. */
    static Path root() {
        Path compose = find("docker-compose.dev.yaml");
        return compose.getParent();
    }
}
