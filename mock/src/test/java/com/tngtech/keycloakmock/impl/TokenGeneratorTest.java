package com.tngtech.keycloakmock.impl;

import static com.tngtech.keycloakmock.api.TokenConfig.aTokenConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import com.tngtech.keycloakmock.impl.dagger.DaggerSignatureComponent;
import com.tngtech.keycloakmock.impl.dagger.SignatureComponent;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import java.net.URI;
import java.net.URISyntaxException;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TokenGeneratorTest {
  private static final String AUDIENCE = "audience";
  private static final String AUTHORIZED_PARTY = "authorized_party";
  private static final List<String> REALM_ROLES = Arrays.asList("peter", "paul");
  private static final List<String> AUDIENCE_ROLES = Arrays.asList("user", "admin");
  private static final String PARTY_ROLE = "party";
  private static final String SUBJECT = "subject";
  private static final String FAMILY = "family";
  private static final String GIVEN = "given";
  private static final String NAME = "name";
  private static final String EMAIL = "email";
  private static final String USERNAME = "username";
  private static final String SCOPE = "scope";
  private static final String CLAIM_KEY = "claim";
  private static final String CLAIM_VALUE = "my claim";
  private static final Instant AUTHENTICATION_TIME = Instant.EPOCH;
  private static final Instant ISSUED_AT = Instant.now().minus(1, ChronoUnit.MINUTES);
  private static final Instant NOT_BEFORE = Instant.now();
  private static final Instant EXPIRATION = Instant.now().plus(10, ChronoUnit.MINUTES);
  private static final String ISSUER = "issuer";
  private static final String AUTHENTICATION_CONTEXT_CLASS_REFERENCE = "acc ref";
  private static final String HOSTNAME = "hostname";
  private static final String REALM = "realm";

  private static SignatureComponent signatureComponent;

  @Mock private UrlConfiguration urlConfiguration;

  private TokenGenerator generator;

  @BeforeAll
  static void initKey() {
    signatureComponent = DaggerSignatureComponent.create();
  }

  @BeforeEach
  void setup() throws URISyntaxException {
    doReturn(urlConfiguration)
        .when(urlConfiguration)
        .forRequestContext(
            ArgumentMatchers.nullable(String.class), ArgumentMatchers.nullable(String.class));
    doReturn(new URI(ISSUER)).when(urlConfiguration).getIssuer();
    // this is a bit awkward, as the token generator is a singleton within the component
    generator = signatureComponent.tokenGenerator();
  }

  @Test
  @SuppressWarnings("unchecked")
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
                .withHostname(HOSTNAME)
                .withRealm(REALM)
                .withFamilyName(FAMILY)
                .withGivenName(GIVEN)
                .withName(NAME)
                .withEmail(EMAIL)
                .withPreferredUsername(USERNAME)
                .withClaim(CLAIM_KEY, CLAIM_VALUE)
                .withClaim("issuer", "ignored")
                .withAuthenticationTime(AUTHENTICATION_TIME)
                .withIssuedAt(ISSUED_AT)
                .withNotBefore(NOT_BEFORE)
                .withExpiration(EXPIRATION)
                .withAuthenticationContextClassReference(AUTHENTICATION_CONTEXT_CLASS_REFERENCE)
                .build(),
            urlConfiguration);

    verify(urlConfiguration).getIssuer();
    verify(urlConfiguration).forRequestContext(HOSTNAME, REALM);
    Jwt<Header<?>, Claims> jwt =
        Jwts.parserBuilder().setSigningKey(signatureComponent.publicKey()).build().parse(token);
    assertThat(jwt.getHeader())
        .containsEntry("alg", "RS256")
        .containsEntry("kid", "keyId")
        .containsEntry("typ", "JWT");
    Claims claims = jwt.getBody();

    assertThat(claims).isEqualTo(generator.parseToken(token));

    assertThat(claims.getExpiration()).isInSameSecondWindowAs(new Date(EXPIRATION.toEpochMilli()));
    assertThat(claims.getIssuedAt()).isInSameSecondWindowAs(new Date(ISSUED_AT.toEpochMilli()));
    assertThat(claims.getNotBefore()).isInSameSecondWindowAs(new Date(NOT_BEFORE.toEpochMilli()));
    assertThat(claims.get("auth_time", Date.class))
        .isInSameSecondWindowAs(new Date(AUTHENTICATION_TIME.toEpochMilli()));
    assertThat(claims.getIssuer()).isEqualTo(ISSUER);
    assertThat(claims.getSubject()).isEqualTo(SUBJECT);
    assertThat(claims)
        .containsEntry("aud", Collections.singletonList(AUDIENCE))
        .containsEntry("azp", AUTHORIZED_PARTY)
        .containsEntry("scope", SCOPE)
        .containsEntry("name", NAME)
        .containsEntry("given_name", GIVEN)
        .containsEntry("family_name", FAMILY)
        .containsEntry("email", EMAIL)
        .containsEntry("preferred_username", USERNAME)
        .containsEntry(CLAIM_KEY, CLAIM_VALUE)
        .containsEntry("acr", AUTHENTICATION_CONTEXT_CLASS_REFERENCE)
        .containsKey("realm_access")
        .containsKey("resource_access");
    assertThat((Map<String, List<String>>) claims.get("realm_access"))
        .containsOnly(entry("roles", REALM_ROLES));
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
  @SuppressWarnings("unchecked")
  void user_data_is_not_generated() {
    String token =
        generator.getToken(aTokenConfig().withSubject("foo.bar").build(), urlConfiguration);

    Jwt<Header<?>, Claims> jwt =
        Jwts.parserBuilder().setSigningKey(signatureComponent.publicKey()).build().parse(token);
    assertThat(jwt.getHeader())
        .containsEntry("alg", "RS256")
        .containsEntry("kid", "keyId")
        .containsEntry("typ", "JWT");
    Claims claims = jwt.getBody();

    assertThat(claims.getSubject()).isEqualTo("foo.bar");
    assertThat(claims)
        .doesNotContainKeys("name", "given_name", "family_name", "email", "preferred_username");
  }

  @Test
  @SuppressWarnings("unchecked")
  void user_data_is_generated() {
    doReturn("example.com").when(urlConfiguration).getHostname();
    String token =
        generator.getToken(
            aTokenConfig().withSubjectAndGeneratedUserData("foo.bar").build(), urlConfiguration);

    Jwt<Header<?>, Claims> jwt =
        Jwts.parserBuilder().setSigningKey(signatureComponent.publicKey()).build().parse(token);
    assertThat(jwt.getHeader())
        .containsEntry("alg", "RS256")
        .containsEntry("kid", "keyId")
        .containsEntry("typ", "JWT");
    Claims claims = jwt.getBody();

    assertThat(claims.getSubject()).isEqualTo("foo.bar");
    assertThat(claims)
        .containsEntry("name", "Foo Bar")
        .containsEntry("given_name", "Foo")
        .containsEntry("family_name", "Bar")
        .containsEntry("email", "foo.bar@example.com")
        .containsEntry("preferred_username", "foo.bar");
  }

  @Test
  @SuppressWarnings("unchecked")
  void explicit_user_data_takes_preference() {
    doReturn("example.com").when(urlConfiguration).getHostname();
    String token =
        generator.getToken(
            aTokenConfig()
                .withSubjectAndGeneratedUserData("foo.bar")
                .withGivenName("Jane")
                .withFamilyName("Doe")
                .withEmail("jane@doe.com")
                .withPreferredUsername("doej")
                .build(),
            urlConfiguration);

    Jwt<Header<?>, Claims> jwt =
        Jwts.parserBuilder().setSigningKey(signatureComponent.publicKey()).build().parse(token);
    assertThat(jwt.getHeader())
        .containsEntry("alg", "RS256")
        .containsEntry("kid", "keyId")
        .containsEntry("typ", "JWT");
    Claims claims = jwt.getBody();

    assertThat(claims.getSubject()).isEqualTo("foo.bar");
    assertThat(claims)
        .containsEntry("name", "Jane Doe")
        .containsEntry("given_name", "Jane")
        .containsEntry("family_name", "Doe")
        .containsEntry("email", "jane@doe.com")
        .containsEntry("preferred_username", "doej");
  }
}
