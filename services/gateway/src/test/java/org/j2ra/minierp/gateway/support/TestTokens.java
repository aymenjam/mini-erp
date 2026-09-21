package org.j2ra.minierp.gateway.support;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
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
 * Makes signed JWTs like Keycloak does, and serves the public key as a JWK set over HTTP, like the jwk-set-uri of Keycloak.
 * So the gateway runs its real decoder (issuer-uri + jwk-set-uri): signature, expiry and issuer are checked for real.
 */
public final class TestTokens {

    public static final String ISSUER = "http://issuer.test/realms/mini-erp";
    private static final String KEY_ID = "test-key";

    private static final KeyPair TRUSTED_KEY = newKey();
    private static final KeyPair OTHER_KEY = newKey();
    private static final HttpServer JWKS_SERVER = startJwksServer();

    private TestTokens() {
    }

    public static String jwkSetUri() {
        return "http://127.0.0.1:" + JWKS_SERVER.getAddress().getPort() + "/certs";
    }

    public static String valid(String... roles) {
        return sign(TRUSTED_KEY, ISSUER, Instant.now().plusSeconds(300), roles);
    }

    /** Expired 10 minutes ago (more than the default clock skew of 60 seconds). */
    public static String expired(String... roles) {
        return sign(TRUSTED_KEY, ISSUER, Instant.now().minusSeconds(600), roles);
    }

    /** Signed with a key that the gateway does not know. Same key id, so the gateway does find a key and must reject the signature. */
    public static String wrongKey(String... roles) {
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
            SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(KEY_ID).build(), claims);
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

    private static HttpServer startJwksServer() {
        try {
            RSAKey publicKey = new RSAKey.Builder((RSAPublicKey) TRUSTED_KEY.getPublic()).keyID(KEY_ID).build();
            byte[] body = new JWKSet(publicKey).toString().getBytes(StandardCharsets.UTF_8);
            HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
            server.createContext("/certs", exchange -> {
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.setExecutor(null);
            // Never stopped: the JVM of the tests ends it (Surefire calls System.exit).
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
