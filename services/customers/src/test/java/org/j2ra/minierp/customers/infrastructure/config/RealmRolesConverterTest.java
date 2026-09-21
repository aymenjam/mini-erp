package org.j2ra.minierp.customers.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** AC-10, plain unit test: no Spring context. */
class RealmRolesConverterTest {

    private final RealmRolesConverter converter = new RealmRolesConverter();

    private static Jwt jwt(Map<String, Object> claims) {
        return new Jwt("token", Instant.now(), Instant.now().plusSeconds(60), Map.of("alg", "none"), claims);
    }

    @Test
    @DisplayName("AC-10: role SALES in realm_access.roles becomes ROLE_SALES")
    void role_becomes_role_authority() {
        Jwt jwt = jwt(Map.of("sub", "u", "realm_access", Map.of("roles", List.of("SALES", "ADMIN"))));
        assertThat(converter.convert(jwt)).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_SALES", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("AC-10: scopes are ignored, only roles give rights")
    void scopes_are_ignored() {
        Jwt jwt = jwt(Map.of("sub", "u", "scope", "openid profile", "realm_access", Map.of("roles", List.of("SALES"))));
        assertThat(converter.convert(jwt)).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_SALES");
    }

    @Test
    @DisplayName("AC-10: roles in resource_access (client roles) are ignored")
    void client_roles_are_ignored() {
        Jwt jwt = jwt(Map.of("sub", "u", "resource_access", Map.of("erp-frontend", Map.of("roles", List.of("ADMIN")))));
        assertThat(converter.convert(jwt)).isEmpty();
    }

    @Test
    @DisplayName("AC-10: a token without realm_access or with odd values gives no authority and no error")
    void missing_or_odd_claims_give_no_authority() {
        assertThat(converter.convert(jwt(Map.of("sub", "u")))).isEmpty();
        assertThat(converter.convert(jwt(Map.of("sub", "u", "realm_access", "not-a-map")))).isEmpty();
        assertThat(converter.convert(jwt(Map.of("sub", "u", "realm_access", Map.of("roles", "not-a-list"))))).isEmpty();
        assertThat(converter.convert(jwt(Map.of("sub", "u", "realm_access", Map.of("roles", List.of(" ", 5)))))).isEmpty();
    }
}
