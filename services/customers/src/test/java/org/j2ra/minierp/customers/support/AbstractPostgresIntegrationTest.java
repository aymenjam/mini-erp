package org.j2ra.minierp.customers.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base of the integration tests: the full app against a throwaway PostgreSQL (Testcontainers).
 * Flyway migrates it when the app starts. The settings use the same variable names as production.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestSecurityBeans.class)
public abstract class AbstractPostgresIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbc;

    // The database is shared by all test classes of the run. Each test starts with no customer, so tests do not depend on each other.
    @BeforeEach
    void emptyCustomerTable() {
        jdbc.execute("TRUNCATE TABLE customer CASCADE");
    }

    @DynamicPropertySource
    static void settings(DynamicPropertyRegistry registry) {
        PostgreSQLContainer<?> postgres = TestPostgres.container();
        registry.add("DB_URL", postgres::getJdbcUrl);
        registry.add("DB_USERNAME", postgres::getUsername);
        registry.add("DB_PASSWORD", postgres::getPassword);
        // Not used to fetch keys in tests (see TestSecurityBeans), but the property must have a value.
        registry.add("ISSUER_URI", () -> TestTokens.ISSUER);
    }
}
