package com.tngtech.keycloakmock.api;

import static com.tngtech.keycloakmock.api.TokenConfig.aTokenConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TokenGeneratorTest {
  private static final String AUDIENCE = "audience";
  private static final String AUTHORIZED_PARTY = "authorized_party";
  private static final List<String> REALM_ROLES = Arrays.asList("peter", "paul");
  private static final List<String> AUDIENCE_ROLES = Arrays.asList("user", "admin");
  private static final String PARTY_ROLE = "party";
  private static final String SUBJECT = "subject";
  private static final String FAMILY = "family";
  private static final String GIVEN = "given";
  private static final String NAME = "given family";
  private static final String EMAIL = "email";
  private static final String USERNAME = "username";
  private static final String SCOPE = "scope";
  private static final String CLAIM_KEY = "claim";
  private static final String CLAIM_VALUE = "my claim";
  private static final Instant AUTHENTICATION_TIME = Instant.EPOCH;
  private static final Instant ISSUED_AT = Instant.now().minus(1, ChronoUnit.MINUTES);
  private static final Instant NOT_BEFORE = Instant.now();
  private static final Instant EXPIRATION = Instant.now().plus(10, ChronoUnit.MINUTES);
  private static final String SOURCE_TOKEN =
      "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICI0aG9XM3JrQ1M0TFRyMXVQa05CNWxEZDVzblJxbFJkcTY3SnFPdGp0RFI4In0.eyJqdGkiOiI5Y2ZlMmI2MC0xZGI5LTRhMTItYjlkYy1kY2FiZmQxNmU5NDUiLCJleHAiOjE1ODY3Nzg2NjAsIm5iZiI6MCwiaWF0IjoxNTg2Nzc4NjAwLCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjgwMDAvYXV0aC9yZWFsbXMvcmVhbG0iLCJhdWQiOiJhY2NvdW50Iiwic3ViIjoiZmJjYWE0MGEtOTQ4MC00ZWFkLWFkZjItOGY2MDg1ZjZmNzVkIiwidHlwIjoiQmVhcmVyIiwiYXpwIjoiY2xpZW50Iiwibm9uY2UiOiJhZWY4Y2YxYi0xMjdkLTQ2NTAtODA4MS0wZGIyNGFmMmQwZTciLCJhdXRoX3RpbWUiOjE1ODY3Nzg1OTksInNlc3Npb25fc3RhdGUiOiIwNTI3MDkwNy1kN2UyLTRiNzUtODM0MS01YmRkOTRlYWI3NjMiLCJhY3IiOiIxIiwiYWxsb3dlZC1vcmlnaW5zIjpbImh0dHA6Ly9sb2NhbGhvc3Q6MzAwMCJdLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsib2ZmbGluZV9hY2Nlc3MiLCJ1bWFfYXV0aG9yaXphdGlvbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7ImFjY291bnQiOnsicm9sZXMiOlsibWFuYWdlLWFjY291bnQiLCJtYW5hZ2UtYWNjb3VudC1saW5rcyIsInZpZXctcHJvZmlsZSJdfX0sInNjb3BlIjoib3BlbmlkIGVtYWlsIHByb2ZpbGUiLCJlbWFpbF92ZXJpZmllZCI6ZmFsc2UsIm5hbWUiOiJQZXRlciBVc2VyIiwicHJlZmVycmVkX3VzZXJuYW1lIjoidXNlciIsImdpdmVuX25hbWUiOiJQZXRlciIsImZhbWlseV9uYW1lIjoiVXNlciIsImVtYWlsIjoidXNlckBrZXljbG9hayJ9.KELcHfTpR_KAFeZdDT3gsfCMdc4dmPNv7MfOEyMXGp-Rz0FPIlILpU6U-Kw674j9oTFVnkWL7Gcj0KsOTi4tsd5fgba7X9If8_gtYGch-kjLxKzTh6gb0Xb8YobFkFzo6lRGJf_u_oKIduRtUw9nPly_55513uayAqIk-Pvx4Yi0pZSxxth_7aTgsraKlKPHIBcKY0-2MX99Ruzf_dndls99um1b7EHDNX8W_ZJA0jFgsQXvl4PFppW0oYkU4XonVU1J3tDCpvorHUEA4IBDgaIyhJFB23EXApJknVf3xaLdvLq_bSTfKF7cN-eBv54cU2h_QscU9Xm9qdV3x-7zAg";
  private static RSAPublicKey key;

  private TokenGenerator generator;

  @BeforeAll
  static void initKey() throws Exception {
    KeyStore keyStore = KeyStore.getInstance("JKS");
    try (InputStream keystoreStream =
        TokenGeneratorTest.class.getResourceAsStream("/keystore.jks")) {
      keyStore.load(keystoreStream, null);
      key = (RSAPublicKey) keyStore.getCertificate("rsa").getPublicKey();
    }
  }

  @BeforeEach
  void setup() {
    generator = new TokenGenerator();
  }

  @Test
  void default_token_is_generated_correctly() {
    String token =
        generator.getToken(aTokenConfig().build(), "http://localhost:8000/auth/realms/master");

    //noinspection unchecked
    Jwt<Header<?>, Claims> jwt = Jwts.parserBuilder().setSigningKey(key).build().parse(token);
    assertThat(jwt.getHeader()).containsEntry("kid", "keyId");
    Claims claims = jwt.getBody();
    assertThat(claims)
        .containsEntry("aud", Collections.singletonList("server"))
        .containsEntry("azp", "client")
        .containsEntry("scope", "openid");
    assertThat(claims.getExpiration()).isInTheFuture();
    assertThat(claims.getNotBefore()).isNull();
    assertThat(claims.getIssuedAt()).isInThePast();
    assertThat(claims.get("auth_time", Long.class)).isLessThanOrEqualTo(Instant.now().getEpochSecond());
    assertThat(claims.getIssuer()).isEqualTo("http://localhost:8000/auth/realms/master");
    assertThat(claims.getSubject()).isEqualTo("user");
  }

  @Test
  void config_is_correctly_applied() {
    String token =
        generator.getToken(
            aTokenConfig()
                .withAudience(AUDIENCE)
                .withAuthorizedParty(AUTHORIZED_PARTY)
                .withSubject(SUBJECT)
                .withScope(SCOPE)
                .withRealmRoles(REALM_ROLES)
                .withResourceRoles(AUDIENCE, AUDIENCE_ROLES)
                .withResourceRole(AUTHORIZED_PARTY, PARTY_ROLE)
                .withFamilyName(FAMILY)
                .withGivenName(GIVEN)
                .withEmail(EMAIL)
                .withPreferredUsername(USERNAME)
                .withClaim(CLAIM_KEY, CLAIM_VALUE)
                .withClaim("issuer", "some issuer")
                .withAuthenticationTime(AUTHENTICATION_TIME)
                .withIssuedAt(ISSUED_AT)
                .withNotBefore(NOT_BEFORE)
                .withExpiration(EXPIRATION)
                .build(),
            "http://localhost:1234/auth/realms/custom");

    //noinspection unchecked
    Jwt<Header<?>, Claims> jwt = Jwts.parserBuilder().setSigningKey(key).build().parse(token);
    assertThat(jwt.getHeader()).containsEntry("kid", "keyId");
    Claims claims = jwt.getBody();
    assertThat(claims.getExpiration()).isInSameSecondWindowAs(new Date(EXPIRATION.toEpochMilli()));
    assertThat(claims.getIssuedAt()).isInSameSecondWindowAs(new Date(ISSUED_AT.toEpochMilli()));
    assertThat(claims.getNotBefore()).isInSameSecondWindowAs(new Date(NOT_BEFORE.toEpochMilli()));
    assertThat(claims.get("auth_time", Date.class))
        .isInSameSecondWindowAs(new Date(AUTHENTICATION_TIME.toEpochMilli()));
    assertThat(claims.getIssuer()).isEqualTo("http://localhost:1234/auth/realms/custom");
    assertThat(claims.getSubject()).isEqualTo(SUBJECT);
    assertThat(claims)
        .containsEntry("aud", Collections.singletonList(AUDIENCE))
        .containsEntry("azp", AUTHORIZED_PARTY)
        .containsEntry("scope", "openid " + SCOPE)
        .containsEntry("name", NAME)
        .containsEntry("given_name", GIVEN)
        .containsEntry("family_name", FAMILY)
        .containsEntry("email", EMAIL)
        .containsEntry("preferred_username", USERNAME)
        .containsEntry(CLAIM_KEY, CLAIM_VALUE)
        .containsKey("realm_access")
        .containsKey("resource_access");
    //noinspection unchecked
    assertThat((Map<String, List<String>>) claims.get("realm_access"))
        .containsOnly(entry("roles", REALM_ROLES));
    //noinspection unchecked
    Map<String, Map<String, List<String>>> resourceAccess =
        (Map<String, Map<String, List<String>>>) claims.get("resource_access");
    assertThat(resourceAccess).containsOnlyKeys(AUDIENCE, AUTHORIZED_PARTY);
    assertThat(resourceAccess.get(AUDIENCE))
        .extractingByKey("roles")
        .asList()
        .containsExactlyInAnyOrderElementsOf(AUDIENCE_ROLES);
    assertThat(resourceAccess.get(AUTHORIZED_PARTY))
        .extractingByKey("roles")
        .asList()
        .containsOnly(PARTY_ROLE);
  }

  @Test
  void config_is_read_correctly_from_original_token() {
    String token =
        generator.getToken(
            aTokenConfig().withSourceToken(SOURCE_TOKEN).build(),
            "http://localhost:8000/auth/realms/master");

    //noinspection unchecked
    Jwt<Header<?>, Claims> jwt = Jwts.parserBuilder().setSigningKey(key).build().parse(token);
    assertThat(jwt.getHeader()).containsEntry("kid", "keyId");
    Claims claims = jwt.getBody();
    assertThat(claims.getExpiration()).isInTheFuture();
    assertThat(claims.getIssuedAt()).isInThePast().isNotEqualTo("2020-04-13T11:50:00Z");
    assertThat(claims.getNotBefore()).isNull();
    assertThat(claims.get("auth_time", Date.class))
        .isInThePast()
        .isNotEqualTo("2020-04-13T11:49:59Z");
    assertThat(claims.getIssuer()).isEqualTo("http://localhost:8000/auth/realms/master");
    assertThat(claims.getSubject()).isEqualTo("fbcaa40a-9480-4ead-adf2-8f6085f6f75d");
    assertThat(claims)
        .containsEntry("jti", "9cfe2b60-1db9-4a12-b9dc-dcabfd16e945")
        .containsEntry("nonce", "aef8cf1b-127d-4650-8081-0db24af2d0e7")
        .containsEntry("session_state", "05270907-d7e2-4b75-8341-5bdd94eab763")
        .containsEntry("acr", "1")
        .containsEntry("allowed-origins", Collections.singletonList("http://localhost:3000"))
        .containsEntry("email_verified", false)
        .containsEntry("aud", Collections.singletonList("account"))
        .containsEntry("azp", "client")
        .containsEntry("name", "Peter User")
        .containsEntry("given_name", "Peter")
        .containsEntry("family_name", "User")
        .containsEntry("email", "user@keycloak")
        .containsEntry("preferred_username", "user")
        .containsKey("realm_access")
        .containsKey("resource_access")
        .extractingByKey("scope")
        .extracting(s -> Arrays.asList(((String) s).split(" ")))
        .asList()
        .containsExactlyInAnyOrder("openid", "email", "profile");
    //noinspection unchecked
    assertThat((Map<String, List<String>>) claims.get("realm_access"))
        .extractingByKey("roles")
        .asList()
        .containsOnly("offline_access", "uma_authorization");
    //noinspection unchecked
    Map<String, Map<String, List<String>>> resourceAccess =
        (Map<String, Map<String, List<String>>>) claims.get("resource_access");
    assertThat(resourceAccess).containsOnlyKeys("account");
    assertThat(resourceAccess.get("account"))
        .extractingByKey("roles")
        .asList()
        .containsOnly("manage-account", "manage-account-links", "view-profile");
  }
}
