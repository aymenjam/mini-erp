package org.j2ra.minierp.customers;

import org.j2ra.minierp.customers.support.TestPostgres;
import org.j2ra.minierp.customers.support.TestTokens;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AC-8. We cannot edit V1 (it is a file in the jar), so we do the same thing from the other side:
 * we change the checksum stored in flyway_schema_history. For Flyway both cases look the same:
 * "the file is not the one that was applied". It uses its own database, so other tests are not affected.
 */
class FlywayValidationTest {

    private static final String DATABASE = "flyway_validation";

    @Test
    @DisplayName("AC-8: the app starts on a clean history and does not start when an applied migration changed")
    void app_does_not_start_when_an_applied_migration_changed() throws SQLException {
        PostgreSQLContainer<?> postgres = TestPostgres.container();
        String url = postgres.getJdbcUrl().replace("/" + postgres.getDatabaseName(), "/" + DATABASE);
        try (Connection admin = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             Statement statement = admin.createStatement()) {
            statement.execute("CREATE DATABASE " + DATABASE);
        }

        // Control: with the unchanged history the app starts (and Flyway applies V1).
        try (ConfigurableApplicationContext context = start(url, postgres)) {
            assertThat(context.isRunning()).isTrue();
        }

        try (Connection connection = DriverManager.getConnection(url, postgres.getUsername(), postgres.getPassword());
             Statement statement = connection.createStatement()) {
            assertThat(statement.executeUpdate("UPDATE flyway_schema_history SET checksum = checksum + 1")).isEqualTo(1);
        }

        assertThatThrownBy(() -> start(url, postgres).close())
                .rootCause()
                .hasMessageContaining("Validate failed")
                .hasMessageContaining("checksum mismatch");
    }

    private ConfigurableApplicationContext start(String url, PostgreSQLContainer<?> postgres) {
        return new SpringApplicationBuilder(CustomersApplication.class)
                .properties(
                        "DB_URL=" + url,
                        "DB_USERNAME=" + postgres.getUsername(),
                        "DB_PASSWORD=" + postgres.getPassword(),
                        "ISSUER_URI=" + TestTokens.ISSUER)
                // A command-line argument wins over application.yaml (which sets the port from SERVER_PORT).
                .run("--server.port=0");
    }
}
