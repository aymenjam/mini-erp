package org.j2ra.minierp.template;

import org.j2ra.minierp.template.support.TestPostgres;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class ComposePostgresVersionTest {

    @Test
    @DisplayName("Spec 0.3 scope 4: the Testcontainers PostgreSQL image equals the image of docker-compose.dev.yaml")
    void test_image_equals_compose_image() throws IOException {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null && !Files.exists(dir.resolve("docker-compose.dev.yaml"))) {
            dir = dir.getParent();
        }
        assertThat(dir).as("docker-compose.dev.yaml found in a parent folder").isNotNull();

        Matcher image = Pattern.compile("(?m)^\\s*image:\\s*(postgres:\\S+)\\s*$")
                .matcher(Files.readString(dir.resolve("docker-compose.dev.yaml")));
        assertThat(image.find()).as("a postgres image line in the compose file").isTrue();
        assertThat(TestPostgres.IMAGE).isEqualTo(image.group(1));
    }
}
