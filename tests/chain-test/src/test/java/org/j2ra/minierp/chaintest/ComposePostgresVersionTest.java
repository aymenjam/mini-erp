package org.j2ra.minierp.chaintest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class ComposePostgresVersionTest {

    @Test
    @DisplayName("Spec 0.4: the PostgreSQL image of the chain test equals the image of docker-compose.dev.yaml")
    void postgres_image_equals_the_compose_image() throws IOException {
        Matcher image = Pattern.compile("(?m)^\\s*image:\\s*(postgres:\\S+)\\s*$")
                .matcher(Files.readString(RepoFiles.find("docker-compose.dev.yaml")));

        assertThat(image.find()).as("a postgres image line in the compose file").isTrue();
        assertThat(ChainEnvironment.POSTGRES_IMAGE).isEqualTo(image.group(1));
    }
}
