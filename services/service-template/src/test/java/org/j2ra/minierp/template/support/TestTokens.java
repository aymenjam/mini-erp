package org.j2ra.minierp.template.support;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Makes signed JWTs like Keycloak does, with a key that only the tests know.
 * The decoder in the tests checks signature, expiry and issuer for real (unlike a mocked jwt()).
 */
public final class TestTokens {

    public static final String ISSUER = "http://issuer.test/realms/mini-erp";

    private static final KeyPair TRUSTED_KEY = newKey();
    private static final KeyPair OTHER_KEY = newKey();

    private TestTokens() {
    }

    public static RSAPublicKey trustedPublicKey() {
        return (RSAPublicKey) TRUSTED_KEY.getPublic();
    }

    /** A valid token with the given realm roles. */
    public static String withRoles(String... roles) {
        return sign(TRUSTED_KEY, ISSUER, Instant.now().plusSeconds(300), roles);
    }

    /** Expired 10 minutes ago (more than the default clock skew of 60 seconds). */
    public static String expired(String... roles) {
        return sign(TRUSTED_KEY, ISSUER, Instant.now().minusSeconds(600), roles);
    }

    /** Signed with a key that the service does not trust. */
    public static String wrongSignature(String... roles) {
        return sign(OTHER_KEY, ISSUER, Instant.now().plusSeconds(300), roles);
    }

    public static String wrongIssuer(String... roles) {
        return sign(TRUSTED_KEY, "http://evil.test/realms/other", Instant.now().plusSeconds(300), roles);
    }

    private static String sign(KeyPair key, String issuer, Instant expiresAt, String... roles) {
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject("test-user")
                    .issuer(issuer)
                    .issueTime(new Date())
                    .expirationTime(Date.from(expiresAt))
                    .claim("realm_access", Map.of("roles", List.of(roles)))
                    .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
            jwt.sign(new RSASSASigner((RSAPrivateKey) key.getPrivate()));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    private static KeyPair newKey() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
