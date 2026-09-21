package org.j2ra.minierp.customers.support;

import org.testcontainers.containers.PostgreSQLContainer;

/**
 * One throwaway PostgreSQL for the whole test run. It is not the docker-compose one.
 * The image tag must equal the tag in docker-compose.dev.yaml (checked by ComposePostgresVersionTest).
 * Testcontainers (Ryuk) removes the container when the JVM ends.
 */
public final class TestPostgres {

    public static final String IMAGE = "postgres:18.6";

    private static PostgreSQLContainer<?> container;

    private TestPostgres() {
    }

    public static synchronized PostgreSQLContainer<?> container() {
        if (container == null) {
            container = new PostgreSQLContainer<>(IMAGE);
            container.start();
        }
        return container;
    }
}
