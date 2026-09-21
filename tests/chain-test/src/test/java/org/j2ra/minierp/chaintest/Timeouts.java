package org.j2ra.minierp.chaintest;

import java.time.Duration;

/** Generous deadlines (a shared CI runner can be slow). Each one can be changed with a system property, for example -Dchaintest.app.timeout.seconds=300. */
final class Timeouts {

    static final String KEYCLOAK_PROPERTY = "chaintest.keycloak.timeout.seconds";
    static final String APP_PROPERTY = "chaintest.app.timeout.seconds";

    private Timeouts() {
    }

    static Duration keycloak() {
        return seconds(KEYCLOAK_PROPERTY, 180);
    }

    static Duration app() {
        return seconds(APP_PROPERTY, 120);
    }

    private static Duration seconds(String property, long defaultSeconds) {
        String value = System.getProperty(property);
        if (value == null || value.isBlank()) {
            return Duration.ofSeconds(defaultSeconds);
        }
        try {
            return Duration.ofSeconds(Long.parseLong(value.trim()));
        } catch (NumberFormatException e) {
            throw new IllegalStateException("The system property " + property + " must be a number of seconds, but it is '" + value + "'");
        }
    }
}
