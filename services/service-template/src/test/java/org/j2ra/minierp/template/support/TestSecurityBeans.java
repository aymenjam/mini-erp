package org.j2ra.minierp.template.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Replaces only the key source: the production decoder gets keys from Keycloak (issuer-uri),
 * the test decoder from TestTokens. Signature, expiry and issuer are checked the same way.
 */
@TestConfiguration
public class TestSecurityBeans {

    @Bean
    JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(TestTokens.trustedPublicKey()).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(TestTokens.ISSUER));
        return decoder;
    }
}
