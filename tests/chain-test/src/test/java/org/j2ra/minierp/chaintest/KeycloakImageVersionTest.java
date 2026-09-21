package org.j2ra.minierp.chaintest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakImageVersionTest {

    @Test
    @DisplayName("Spec 0.4: the Keycloak image of the chain test equals the image of docker-compose.dev.yaml")
    void keycloak_image_equals_the_compose_image() throws IOException {
        Matcher image = Pattern.compile("(?m)^\\s*image:\\s*(quay\\.io/keycloak/keycloak:\\S+)\\s*$")
                .matcher(Files.readString(RepoFiles.find("docker-compose.dev.yaml")));

        assertThat(image.find()).as("a Keycloak image line in the compose file").isTrue();
        assertThat(KeycloakSupport.IMAGE).isEqualTo(image.group(1));
    }
}
