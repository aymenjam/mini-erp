package org.j2ra.minierp.chaintest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * One Spring Boot application started from its jar, in its own JVM (as in production: own application.yaml, own classpath).
 * It is always stopped: close() kills it, and a shutdown hook kills it if the test JVM ends in a hurry.
 */
final class AppProcess implements AutoCloseable {

    /** Two applications, a Keycloak and a PostgreSQL must fit on a shared CI runner. */
    static final List<String> JVM_OPTIONS = List.of("-Xmx256m", "-XX:+UseSerialGC");

    private final String name;
    private final int port;
    private final Path logFile;
    private final Process process;
    private final Thread shutdownHook;

    private AppProcess(String name, int port, Path logFile, Process process) {
        this.name = name;
        this.port = port;
        this.logFile = logFile;
        this.process = process;
        // If the test JVM is killed or ends without close(), the application must not stay alive.
        this.shutdownHook = new Thread(this::kill, "stop-" + name);
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    /**
     * Starts the jar with the java of the current JVM (not the one on the PATH). The environment is empty except for the given
     * variables, so nothing of the machine (for example a .env that was loaded in the shell) can change the result.
     */
    static AppProcess start(String name, Path jar, int port, Map<String, String> environment, Path logDirectory) {
        try {
            Files.createDirectories(logDirectory);
            Path logFile = logDirectory.resolve(name + ".log");
            List<String> command = new java.util.ArrayList<>();
            command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
            command.addAll(JVM_OPTIONS);
            command.add("-jar");
            command.add(jar.toString());

            ProcessBuilder builder = new ProcessBuilder(command);
            builder.environment().clear();
            builder.environment().putAll(environment);
            builder.redirectErrorStream(true);
            builder.redirectOutput(ProcessBuilder.Redirect.to(logFile.toFile()));
            return new AppProcess(name, port, logFile, builder.start());
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot start " + name + " from " + jar, e);
        }
    }

    String name() {
        return name;
    }

    int port() {
        return port;
    }

    Path logFile() {
        return logFile;
    }

    String baseUrl() {
        return "http://127.0.0.1:" + port;
    }

    /** Polls /actuator/health until it says UP. Stops at once, with the log, if the process ended. No fixed sleep: it ends as soon as the answer is UP. */
    void awaitHealthy(Duration timeout) {
        URI health = URI.create(baseUrl() + "/actuator/health");
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            if (!process.isAlive()) {
                throw new AssertionError(name + " stopped with exit code " + process.exitValue() + " before it was healthy.\n" + logTail(60));
            }
            try {
                HttpResponse<String> response = client.send(HttpRequest.newBuilder(health).timeout(Duration.ofSeconds(2)).build(),
                        HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200 && response.body().contains("\"UP\"")) {
                    return;
                }
            } catch (IOException e) {
                // Not listening yet. Try again until the deadline.
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for " + name, e);
            }
            pause(Duration.ofMillis(300));
        }
        throw new AssertionError("TIMEOUT: " + name + " was not healthy at " + health + " after " + timeout.toSeconds() + " s"
                + " (change it with -D" + Timeouts.APP_PROPERTY + "=<seconds>).\n" + logTail(60));
    }

    String logTail(int lines) {
        try {
            if (!Files.exists(logFile)) {
                return "(no log file " + logFile + ")";
            }
            List<String> all = Files.readAllLines(logFile);
            return "--- last " + Math.min(lines, all.size()) + " lines of " + logFile + "\n"
                    + String.join("\n", all.subList(Math.max(0, all.size() - lines), all.size()));
        } catch (IOException e) {
            return "(cannot read " + logFile + ": " + e + ")";
        }
    }

    boolean isAlive() {
        return process.isAlive();
    }

    @Override
    public void close() {
        kill();
        try {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
        } catch (IllegalStateException e) {
            // The JVM is already shutting down, and the hook does the same work.
        }
    }

    private void kill() {
        process.descendants().forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
        try {
            process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void pause(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
