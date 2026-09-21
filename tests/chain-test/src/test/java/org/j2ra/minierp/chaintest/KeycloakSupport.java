package org.j2ra.minierp.chaintest;

import com.jayway.jsonpath.JsonPath;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/** Keycloak for the chain test: a Testcontainers GenericContainer with the realm file of the repository. */
final class KeycloakSupport {

    /** Must equal the tag in docker-compose.dev.yaml (checked by KeycloakImageVersionTest). */
    static final String IMAGE = "quay.io/keycloak/keycloak:26.7.4";
    static final String REALM = "mini-erp";
    static final String REALM_FILE = "infra/keycloak/mini-erp-realm.json";
    static final int HTTP_PORT = 8080;

    // Fake values. The realm file has a ${DEV_USER_PASSWORD} placeholder that Keycloak fills from this variable.
    static final String DEV_USER_PASSWORD = "chain-test-user-pw";
    private static final String ADMIN_USER = "chain-admin";
    private static final String ADMIN_PASSWORD = "chain-test-admin-pw";

    private static final Pattern IPV4 = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3}");

    private KeycloakSupport() {
    }

    /**
     * No KC_HOSTNAME on purpose. The compose file sets KC_HOSTNAME=http://localhost:8080, but here the port is random and the host can be
     * "docker" (dind in the pipeline). Without a hostname, start-dev builds the issuer from the address of each request (Keycloak 26, hostname v2).
     * The realm file is copied into the container: a bind mount does not work when the Docker daemon is on another machine (dind).
     */
    static GenericContainer<?> newContainer(Path realmFile) {
        return new GenericContainer<>(DockerImageName.parse(IMAGE))
                .withCommand("start-dev", "--import-realm")
                .withEnv("KC_BOOTSTRAP_ADMIN_USERNAME", ADMIN_USER)
                .withEnv("KC_BOOTSTRAP_ADMIN_PASSWORD", ADMIN_PASSWORD)
                .withEnv("DEV_USER_PASSWORD", DEV_USER_PASSWORD)
                .withCopyFileToContainer(MountableFile.forHostPath(realmFile), "/opt/keycloak/data/import/mini-erp-realm.json")
                .withExposedPorts(HTTP_PORT)
                // The realm answers only after the import is done.
                .waitingFor(Wait.forHttp("/realms/" + REALM).forPort(HTTP_PORT).forStatusCode(200).withStartupTimeout(Timeouts.keycloak()));
    }

    static String baseUrl(String host, int port) {
        return "http://" + host + ":" + port;
    }

    static String issuer(String baseUrl) {
        return baseUrl + "/realms/" + REALM;
    }

    static String jwkSetUri(String baseUrl) {
        return issuer(baseUrl) + "/protocol/openid-connect/certs";
    }

    /** A token of a dev user from the client erp-test-cli (password grant). The issuer of the token is the address used for this request. */
    static String token(String baseUrl, String user) {
        Http.Response response = Http.postForm(issuer(baseUrl) + "/protocol/openid-connect/token", form(user));
        if (response.status() != 200) {
            throw new IllegalStateException("Keycloak gave no token for '" + user + "' at " + baseUrl + ": HTTP " + response.status() + " " + response.body());
        }
        return JsonPath.read(response.body(), "$.access_token");
    }

    /** The "iss" claim of a token (the middle part of the JWT). */
    static String issuerOf(String token) {
        String payload = new String(java.util.Base64.getUrlDecoder().decode(token.split("\\.")[1]), java.nio.charset.StandardCharsets.UTF_8);
        return JsonPath.read(payload, "$.iss");
    }

    /**
     * Another name for the same machine, so the issuer text differs and Keycloak is still reachable.
     * A host name gives its IPv4 address (in the pipeline: "docker" gives the IP of the Docker host).
     * An IP literal gives a name: localhost for 127.0.0.1, 127.0.0.1 for localhost, else the name found by reverse lookup.
     */
    static String alternativeHost(String host) {
        try {
            if (host.equals("localhost")) {
                return "127.0.0.1";
            }
            if (host.equals("127.0.0.1")) {
                return "localhost";
            }
            if (IPV4.matcher(host).matches()) {
                String name = InetAddress.getByName(host).getCanonicalHostName();
                if (!name.equals(host)) {
                    return name;
                }
                throw noAlternative(host, "it is an IP address and it has no other name (reverse lookup gave the same text)");
            }
            for (InetAddress address : InetAddress.getAllByName(host)) {
                if (address instanceof Inet4Address) {
                    return address.getHostAddress();
                }
            }
            throw noAlternative(host, "it has no IPv4 address");
        } catch (UnknownHostException e) {
            throw noAlternative(host, "it cannot be resolved (" + e.getMessage() + ")");
        }
    }

    private static IllegalStateException noAlternative(String host, String reason) {
        return new IllegalStateException("The 'other issuer' check needs a second name for the Keycloak host '" + host + "', but " + reason
                + ". Set TESTCONTAINERS_HOST_OVERRIDE to a host name or to localhost.");
    }

    private static Map<String, String> form(String user) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "password");
        form.put("client_id", "erp-test-cli");
        form.put("username", user);
        form.put("password", DEV_USER_PASSWORD);
        return form;
    }
}
