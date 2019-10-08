package com.tngtech.keycloakmock.api;

import static com.tngtech.keycloakmock.api.TokenConfig.aTokenConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TokenGeneratorTest {
  private static final String AUDIENCE = "audience";
  private static final String AUTHORIZED_PARTY = "authorized_party";
  private static final List<String> REALM_ROLES =
      Stream.of("peter", "paul").collect(Collectors.toList());
  private static final List<String> AUDIENCE_ROLES =
      Stream.of("user", "admin").collect(Collectors.toList());
  private static final String PARTY_ROLE = "party";
  private static final String SUBJECT = "subject";
  private static final String FAMILY = "family";
  private static final String GIVEN = "given";
  private static final String NAME = "given family";
  private static final String EMAIL = "email";
  private static final String USERNAME = "username";
  private static final String CLAIM_KEY = "claim";
  private static final String CLAIM_VALUE = "my claim";
  private static final Instant ISSUED_AT = Instant.EPOCH;
  private static final Instant EXPIRATION = Instant.now().plus(1, ChronoUnit.MINUTES);
  private static RSAPublicKey key;

  @BeforeAll
  static void initKey() throws Exception {
    KeyStore keyStore = KeyStore.getInstance("JKS");
    try (InputStream keystoreStream =
        TokenGeneratorTest.class.getResourceAsStream("/keystore.jks")) {
      keyStore.load(keystoreStream, null);
      key = (RSAPublicKey) keyStore.getCertificate("rsa").getPublicKey();
    }
  }

  @Test
  void default_token_is_generated_correctly() {
    TokenGenerator generator = new TokenGenerator();
    final String token =
        generator.getToken(aTokenConfig().build(), "http://localhost:8000/auth/realms/master");

    try {
      Jwt jwt = Jwts.parser().setSigningKey(key).parse(token);
      assertThat(jwt.getHeader().get("kid")).isEqualTo("keyId");
      assertThat(jwt.getBody()).isInstanceOf(Claims.class);
      Claims claims = (Claims) jwt.getBody();
      //noinspection unchecked
      assertThat((Collection<String>) claims.get("aud", Collection.class))
          .containsExactly("server");
      assertThat(claims.get("azp", String.class)).isEqualTo("client");
      assertThat(claims.getExpiration()).isInTheFuture();
      assertThat(claims.getIssuedAt()).isInThePast();
      assertThat(claims.getIssuer()).isEqualTo("http://localhost:8000/auth/realms/master");
      assertThat(claims.getSubject()).isEqualTo("user");
    } catch (RuntimeException e) {
      Assertions.fail("No exception should have been thrown", e);
    }
  }

  @Test
  void config_is_correctly_applied() {
    TokenGenerator generator = new TokenGenerator();
    final String token =
        generator.getToken(
            aTokenConfig()
                .withAudience(AUDIENCE)
                .withAuthorizedParty(AUTHORIZED_PARTY)
                .withSubject(SUBJECT)
                .withRealmRoles(REALM_ROLES)
                .withResourceRoles(AUDIENCE, AUDIENCE_ROLES)
                .withResourceRole(AUTHORIZED_PARTY, PARTY_ROLE)
                .withFamilyName(FAMILY)
                .withGivenName(GIVEN)
                .withEmail(EMAIL)
                .withPreferredUsername(USERNAME)
                .withClaim(CLAIM_KEY, CLAIM_VALUE)
                .withClaim("issuer", "some issuer")
                .withIssuedAt(ISSUED_AT)
                .withExpiration(EXPIRATION)
                .build(),
            "http://localhost:1234/auth/realms/custom");

    try {
      Jwt jwt = Jwts.parser().setSigningKey(key).parse(token);
      assertThat(jwt.getHeader().get("kid")).isEqualTo("keyId");
      assertThat(jwt.getBody()).isInstanceOf(Claims.class);
      Claims claims = (Claims) jwt.getBody();
      //noinspection unchecked
      assertThat((Collection<String>) claims.get("aud", Collection.class))
          .containsExactly(AUDIENCE);
      assertThat(claims.get("azp")).isEqualTo(AUTHORIZED_PARTY);
      assertThat(claims.getExpiration())
          .isEqualToIgnoringMillis(new Date(EXPIRATION.toEpochMilli()));
      assertThat(claims.getIssuedAt()).isEqualToIgnoringMillis(new Date(ISSUED_AT.toEpochMilli()));
      assertThat(claims.getIssuer()).isEqualTo("http://localhost:1234/auth/realms/custom");
      assertThat(claims.getSubject()).isEqualTo(SUBJECT);
      assertThat(claims.get("name")).isEqualTo(NAME);
      assertThat(claims.get("given_name")).isEqualTo(GIVEN);
      assertThat(claims.get("family_name")).isEqualTo(FAMILY);
      assertThat(claims.get("email")).isEqualTo(EMAIL);
      assertThat(claims.get("preferred_username")).isEqualTo(USERNAME);
      //noinspection unchecked
      assertThat((Map<String, List<String>>) claims.get("realm_access", Map.class))
          .containsOnly(entry("roles", REALM_ROLES));
      //noinspection unchecked
      Map<String, Map<String, List<String>>> resourceAccess =
          (Map<String, Map<String, List<String>>>) claims.get("resource_access", Map.class);
      assertThat(resourceAccess).containsOnlyKeys(AUDIENCE, AUTHORIZED_PARTY);
      assertThat(resourceAccess.get(AUDIENCE).get("roles"))
          .containsExactlyInAnyOrderElementsOf(AUDIENCE_ROLES);
      assertThat(resourceAccess.get(AUTHORIZED_PARTY).get("roles")).containsExactly(PARTY_ROLE);
      assertThat(claims.get(CLAIM_KEY)).isEqualTo(CLAIM_VALUE);
    } catch (RuntimeException e) {
      Assertions.fail("No exception should have been thrown", e);
    }
  }
}
