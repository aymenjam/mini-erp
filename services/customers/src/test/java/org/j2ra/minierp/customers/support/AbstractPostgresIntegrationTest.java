package org.j2ra.minierp.customers.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
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
