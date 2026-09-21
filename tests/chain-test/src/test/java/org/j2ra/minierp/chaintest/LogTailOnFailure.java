package org.j2ra.minierp.chaintest;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.LifecycleMethodExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** When a test (or the start of the chain) fails, the last lines of the two application logs go to the test output. Then the failure goes on. */
final class LogTailOnFailure implements TestExecutionExceptionHandler, LifecycleMethodExecutionExceptionHandler {

    private static final int LINES = 60;

    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        printTails("test " + context.getDisplayName());
        throw throwable;
    }

    @Override
    public void handleBeforeAllMethodExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        printTails("start of the chain");
        throw throwable;
    }

    static void printTails(String reason) {
        System.err.println("=== Chain test failed (" + reason + "). End of the application logs:");
        for (String name : List.of("customers", "gateway")) {
            Path log = ChainEnvironment.logDirectory().resolve(name + ".log");
            try {
                if (!Files.exists(log)) {
                    System.err.println("--- " + name + ": no log file (" + log + ")");
                    continue;
                }
                List<String> lines = Files.readAllLines(log);
                System.err.println("--- " + name + " (last " + Math.min(LINES, lines.size()) + " lines of " + log + ")");
                lines.subList(Math.max(0, lines.size() - LINES), lines.size()).forEach(System.err::println);
            } catch (IOException e) {
                System.err.println("--- " + name + ": cannot read " + log + ": " + e);
            }
        }
    }
}
