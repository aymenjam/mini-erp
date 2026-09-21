package org.j2ra.minierp.chaintest;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/** AC-19: erp-test-cli (password grant) is defined once, in the dev realm, marked dev and test only, and the README says it must never be in production. */
class RealmFileTest {

    private static final Pattern TEST_CLIENT_DEFINITION = Pattern.compile("\"clientId\"\\s*:\\s*\"erp-test-cli\"");

    @Test
    @DisplayName("AC-19: erp-test-cli is defined once in the whole infra folder, and it is the dev realm file")
    void test_client_is_defined_once() throws IOException {
        Path realmFile = RepoFiles.find(KeycloakSupport.REALM_FILE);
        Path infra = RepoFiles.root().resolve("infra");

        try (Stream<Path> files = Files.walk(infra)) {
            List<Path> definitions = files.filter(path -> path.toString().endsWith(".json")).filter(RealmFileTest::definesTestClient).toList();
            assertThat(definitions).containsExactly(realmFile);
        }
        List<Map<String, Object>> clients = JsonPath.read(Files.readString(realmFile), "$.clients[?(@.clientId == 'erp-test-cli')]");
        assertThat(clients).as("clients named erp-test-cli in the realm file").hasSize(1);
    }

    @Test
    @DisplayName("AC-19: erp-test-cli is marked dev and test only, and it is the only client with the password grant")
    void test_client_is_marked_and_alone_with_the_password_grant() throws IOException {
        String realm = Files.readString(RepoFiles.find(KeycloakSupport.REALM_FILE));

        assertThat(JsonPath.<List<String>>read(realm, "$.clients[?(@.clientId == 'erp-test-cli')].name").get(0)).containsIgnoringCase("dev and test only");
        assertThat(JsonPath.<List<Boolean>>read(realm, "$.clients[?(@.clientId == 'erp-test-cli')].directAccessGrantsEnabled")).containsExactly(true);
        assertThat(JsonPath.<List<String>>read(realm, "$.clients[?(@.directAccessGrantsEnabled == true)].clientId")).containsExactly("erp-test-cli");
    }

    @Test
    @DisplayName("AC-19: README.md, in the Keycloak section, says that erp-test-cli must never exist in a production realm")
    void readme_says_never_in_production() throws IOException {
        String readme = Files.readString(RepoFiles.root().resolve("README.md"));
        int start = readme.indexOf("\n## Keycloak");
        assertThat(start).as("a 'Keycloak' section in README.md").isNotNegative();
        int end = readme.indexOf("\n## ", start + 1);
        String section = readme.substring(start, end < 0 ? readme.length() : end).toLowerCase().replaceAll("\\s+", " ");

        assertThat(section).contains("erp-test-cli").contains("must never exist in a production realm");
    }

    private static boolean definesTestClient(Path file) {
        try {
            return TEST_CLIENT_DEFINITION.matcher(Files.readString(file)).find();
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }
}
