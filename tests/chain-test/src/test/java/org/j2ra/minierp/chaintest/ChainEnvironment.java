package org.j2ra.minierp.chaintest;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The whole chain: PostgreSQL and Keycloak in containers, customers and gateway as processes.
 * Everything that was started is stopped again by close(), also when a later step of start() fails.
 * The addresses of Keycloak (issuer, jwk-set-uri, token) all come from container.getHost() and the mapped port,
 * so it works when the containers are not on 127.0.0.1 (TESTCONTAINERS_HOST_OVERRIDE=docker in the pipeline).
 */
final class ChainEnvironment implements AutoCloseable {

    /** Must equal the tag in docker-compose.dev.yaml (checked by ComposePostgresVersionTest). */
    static final String POSTGRES_IMAGE = "postgres:18.6";
    static final String ALLOWED_ORIGIN = "http://localhost:4200";

    private static final String DB_NAME = "customers";
    private static final String DB_USER = "customers";
    private static final String DB_PASSWORD = "chain-test-db-pw";

    private final Deque<AutoCloseable> started = new ArrayDeque<>();

    private PostgreSQLContainer<?> postgres;
    private GenericContainer<?> keycloak;
    private AppProcess customers;
    private AppProcess gateway;
    private String keycloakHost;
    private String keycloakBaseUrl;

    private ChainEnvironment() {
    }

    static Path logDirectory() {
        return RepoFiles.moduleDir().resolve("target/chain-test-logs");
    }

    static ChainEnvironment start() {
        ChainEnvironment environment = new ChainEnvironment();
        try {
            environment.startAll();
            return environment;
        } catch (RuntimeException | Error e) {
            environment.close();
            throw e;
        }
    }

    private void startAll() {
        Path gatewayJar = JarLocator.gateway();
        Path customersJar = JarLocator.customers();
        Path realmFile = RepoFiles.find(KeycloakSupport.REALM_FILE);

        postgres = new PostgreSQLContainer<>(POSTGRES_IMAGE).withDatabaseName(DB_NAME).withUsername(DB_USER).withPassword(DB_PASSWORD);
        startContainer("PostgreSQL", postgres);

        keycloak = KeycloakSupport.newContainer(realmFile);
        startContainer("Keycloak", keycloak);
        keycloakHost = keycloak.getHost();
        keycloakBaseUrl = KeycloakSupport.baseUrl(keycloakHost, keycloak.getMappedPort(KeycloakSupport.HTTP_PORT));

        String issuer = KeycloakSupport.issuer(keycloakBaseUrl);

        int customersPort = freePort();
        Map<String, String> customersEnvironment = new LinkedHashMap<>();
        customersEnvironment.put("SPRING_PROFILES_ACTIVE", "dev");
        customersEnvironment.put("SERVER_PORT", String.valueOf(customersPort));
        customersEnvironment.put("DB_URL", postgres.getJdbcUrl());
        customersEnvironment.put("DB_USERNAME", DB_USER);
        customersEnvironment.put("DB_PASSWORD", DB_PASSWORD);
        customersEnvironment.put("ISSUER_URI", issuer);
        // customers does not log the id itself. Spring's request line (DEBUG, "GET /api/v1/customers") has it, through the log pattern.
        // customers is not changed for this. A package name, not the class: Boot turns the name of an environment variable into lower case,
        // and a logger name is case-sensitive, so ..._DISPATCHERSERVLET would not match DispatcherServlet.
        customersEnvironment.put("LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_WEB_SERVLET", "DEBUG");
        customers = AppProcess.start("customers", customersJar, customersPort, customersEnvironment, logDirectory());
        started.push(customers);

        int gatewayPort = freePort();
        Map<String, String> gatewayEnvironment = new LinkedHashMap<>();
        gatewayEnvironment.put("SPRING_PROFILES_ACTIVE", "dev");
        gatewayEnvironment.put("SERVER_PORT", String.valueOf(gatewayPort));
        gatewayEnvironment.put("ISSUER_URI", issuer);
        gatewayEnvironment.put("JWK_SET_URI", KeycloakSupport.jwkSetUri(keycloakBaseUrl));
        gatewayEnvironment.put("CUSTOMERS_URI", customers.baseUrl());
        gatewayEnvironment.put("CORS_ALLOWED_ORIGIN", ALLOWED_ORIGIN);
        gateway = AppProcess.start("gateway", gatewayJar, gatewayPort, gatewayEnvironment, logDirectory());
        started.push(gateway);

        // Both start at the same time. Waiting for one after the other only checks them in turn.
        customers.awaitHealthy(Timeouts.app());
        gateway.awaitHealthy(Timeouts.app());
    }

    private void startContainer(String name, GenericContainer<?> container) {
        started.push(container::stop);
        try {
            container.start();
        } catch (RuntimeException e) {
            throw new IllegalStateException("TIMEOUT or failure while starting " + name + " (" + container.getDockerImageName()
                    + "). Deadline for Keycloak: " + Timeouts.keycloak().toSeconds() + " s (change it with -D" + Timeouts.KEYCLOAK_PROPERTY
                    + "=<seconds>).\nContainer log (end):\n" + tail(safeLogs(container), 60), e);
        }
    }

    private static String safeLogs(GenericContainer<?> container) {
        try {
            return container.getLogs();
        } catch (RuntimeException e) {
            return "(no container log: " + e + ")";
        }
    }

    private static String tail(String text, int lines) {
        String[] all = text.split("\\R");
        int from = Math.max(0, all.length - lines);
        return String.join("\n", java.util.Arrays.copyOfRange(all, from, all.length));
    }

    private static int freePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException("No free port", e);
        }
    }

    String gatewayUrl() {
        return gateway.baseUrl();
    }

    String customersUrl() {
        return customers.baseUrl();
    }

    Path gatewayLog() {
        return gateway.logFile();
    }

    Path customersLog() {
        return customers.logFile();
    }

    /** The issuer that both applications expect. */
    String expectedIssuer() {
        return KeycloakSupport.issuer(keycloakBaseUrl);
    }

    /** A token whose issuer is the expected one (the token request goes to the same address as the issuer). */
    String token(String user) {
        return KeycloakSupport.token(keycloakBaseUrl, user);
    }

    /** A token that Keycloak signs correctly, but whose issuer is another name for the same Keycloak. */
    String tokenFromOtherIssuer(String user) {
        String otherHost = KeycloakSupport.alternativeHost(keycloakHost);
        return KeycloakSupport.token(KeycloakSupport.baseUrl(otherHost, keycloak.getMappedPort(KeycloakSupport.HTTP_PORT)), user);
    }

    @Override
    public void close() {
        // Last started, first stopped. One failure must not stop the others from being stopped.
        while (!started.isEmpty()) {
            try {
                started.pop().close();
            } catch (Exception e) {
                System.err.println("Cannot stop a part of the chain: " + e);
            }
        }
    }
}
