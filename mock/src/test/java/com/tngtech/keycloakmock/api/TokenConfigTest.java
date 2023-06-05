package com.tngtech.keycloakmock.api;

import static com.tngtech.keycloakmock.api.TokenConfig.aTokenConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.within;

import com.tngtech.keycloakmock.api.TokenConfig.Builder;
import io.jsonwebtoken.lang.Maps;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TokenConfigTest {
  private static final String KEYCLOAK_SOURCE_TOKEN =
      "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICI0aG9XM3JrQ1M0TFRyMXVQa05CNWxEZDVzblJxbFJkcTY3SnFPdGp0RFI4In0.eyJqdGkiOiI5Y2ZlMmI2MC0xZGI5LTRhMTItYjlkYy1kY2FiZmQxNmU5NDUiLCJleHAiOjE1ODY3Nzg2NjAsIm5iZiI6MCwiaWF0IjoxNTg2Nzc4NjAwLCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjgwMDAvYXV0aC9yZWFsbXMvcmVhbG0iLCJhdWQiOiJhY2NvdW50Iiwic3ViIjoiZmJjYWE0MGEtOTQ4MC00ZWFkLWFkZjItOGY2MDg1ZjZmNzVkIiwidHlwIjoiQmVhcmVyIiwiYXpwIjoiY2xpZW50Iiwibm9uY2UiOiJhZWY4Y2YxYi0xMjdkLTQ2NTAtODA4MS0wZGIyNGFmMmQwZTciLCJhdXRoX3RpbWUiOjE1ODY3Nzg1OTksInNlc3Npb25fc3RhdGUiOiIwNTI3MDkwNy1kN2UyLTRiNzUtODM0MS01YmRkOTRlYWI3NjMiLCJhY3IiOiIxIiwiYWxsb3dlZC1vcmlnaW5zIjpbImh0dHA6Ly9sb2NhbGhvc3Q6MzAwMCJdLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsib2ZmbGluZV9hY2Nlc3MiLCJ1bWFfYXV0aG9yaXphdGlvbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7ImFjY291bnQiOnsicm9sZXMiOlsibWFuYWdlLWFjY291bnQiLCJtYW5hZ2UtYWNjb3VudC1saW5rcyIsInZpZXctcHJvZmlsZSJdfX0sInNjb3BlIjoib3BlbmlkIGVtYWlsIHByb2ZpbGUiLCJlbWFpbF92ZXJpZmllZCI6ZmFsc2UsIm5hbWUiOiJQZXRlciBVc2VyIiwicHJlZmVycmVkX3VzZXJuYW1lIjoidXNlciIsImdpdmVuX25hbWUiOiJQZXRlciIsImZhbWlseV9uYW1lIjoiVXNlciIsImVtYWlsIjoidXNlckBrZXljbG9hayJ9.KELcHfTpR_KAFeZdDT3gsfCMdc4dmPNv7MfOEyMXGp-Rz0FPIlILpU6U-Kw674j9oTFVnkWL7Gcj0KsOTi4tsd5fgba7X9If8_gtYGch-kjLxKzTh6gb0Xb8YobFkFzo6lRGJf_u_oKIduRtUw9nPly_55513uayAqIk-Pvx4Yi0pZSxxth_7aTgsraKlKPHIBcKY0-2MX99Ruzf_dndls99um1b7EHDNX8W_ZJA0jFgsQXvl4PFppW0oYkU4XonVU1J3tDCpvorHUEA4IBDgaIyhJFB23EXApJknVf3xaLdvLq_bSTfKF7cN-eBv54cU2h_QscU9Xm9qdV3x-7zAg";
  private static final String TOKEN_WITH_INVALID_ISSUER_URL =
      "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJpc3MiOiJubyB1cmwifQ.AEpkjo0rde_hsN6e4c8UfH3qqV82FkCgnE6_G8Op1HI";
  private static final String TOKEN_WITH_UNEXPECTED_ISSUER_URL =
      "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJpc3MiOiJodHRwOi8vbG9jYWxob3N0L2F1dGgifQ.Bt7a82iTzNsWO2GtOEAcoIGwEH4g4OxLNnGWhJnTego";

  @Test
  void default_values_are_used() {
    TokenConfig config = aTokenConfig().build();
    Instant now = Instant.now();

    assertThat(config.getAudience()).containsExactly("server");
    assertThat(config.getAuthenticationTime()).isBetween(now.minusSeconds(1), now);
    assertThat(config.getAuthorizedParty()).isEqualTo("client");
    assertThat(config.getClaims()).isEmpty();
    assertThat(config.getEmail()).isNull();
    assertThat(config.getExpiration())
        .isCloseTo(now.plus(10, ChronoUnit.HOURS), within(1, ChronoUnit.SECONDS));
    assertThat(config.getFamilyName()).isNull();
    assertThat(config.getGivenName()).isNull();
    assertThat(config.getIssuedAt()).isBetween(now.minusSeconds(1), now);
    assertThat(config.getNotBefore()).isNull();
    assertThat(config.getHostname()).isNull();
    assertThat(config.getRealm()).isNull();
    assertThat(config.getName()).isNull();
    assertThat(config.getPreferredUsername()).isNull();
    assertThat(config.getRealmAccess().getRoles()).isEmpty();
    assertThat(config.getResourceAccess()).isEmpty();
    assertThat(config.getScope()).isEmpty();
    assertThat(config.getSubject()).isEqualTo("user");
    assertThat(config.getAuthenticationContextClassReference()).isNull();
    assertThat(config.isGenerateUserDataFromSubject()).isFalse();
  }

  @Test
  void audience_is_set_correctly() {
    TokenConfig config =
        aTokenConfig()
            .withAudience("audience1")
            .withAudiences(Collections.singleton("audience2"))
            .withAudience("audience3")
            .build();

    assertThat(config.getAudience())
        .containsExactlyInAnyOrder("audience1", "audience2", "audience3");
  }

  @Test
  void authentication_context_class_reference_is_set_correctly() {
    TokenConfig config = aTokenConfig().withAuthenticationContextClassReference("0").build();

    assertThat(config.getAuthenticationContextClassReference()).isEqualTo("0");
  }

  @Test
  void authenticationTime_is_set_correctly() {
    Instant authenticationTime = Instant.now().minusSeconds(5);
    TokenConfig config = aTokenConfig().withAuthenticationTime(authenticationTime).build();

    assertThat(config.getAuthenticationTime())
        .isCloseTo(authenticationTime, within(1, ChronoUnit.MILLIS));
  }

  @Test
  void authorizedParty_is_set_correctly() {
    TokenConfig config = aTokenConfig().withAuthorizedParty("authorized").build();

    assertThat(config.getAuthorizedParty()).isEqualTo("authorized");
  }

  @Test
  void custom_claims_are_set_correctly() {
    TokenConfig config =
        aTokenConfig()
            .withClaim("claim1", 1)
            .withClaims(Maps.<String, Object>of("claim2", "2").build())
            .withClaim("claim3", true)
            .build();

    assertThat(config.getClaims())
        .containsOnly(entry("claim1", 1), entry("claim2", "2"), entry("claim3", true));
  }

  @Test
  void last_claim_version_wins() {
    TokenConfig config =
        aTokenConfig()
            .withClaim("claim", 1)
            .withClaims(Maps.<String, Object>of("claim", "2").build())
            .withClaim("claim", true)
            .build();

    assertThat(config.getClaims()).containsOnly(entry("claim", true));
  }

  @Test
  void email_is_set_correctly() {
    TokenConfig config = aTokenConfig().withEmail("email").build();

    assertThat(config.getEmail()).isEqualTo("email");
  }

  @Test
  void expiration_is_set_correctly() {
    Instant expiration = Instant.now().plusSeconds(5);
    TokenConfig config = aTokenConfig().withExpiration(expiration).build();

    assertThat(config.getExpiration()).isCloseTo(expiration, within(1, ChronoUnit.MILLIS));
  }

  @Test
  void names_are_set_correctly() {
    TokenConfig config =
        aTokenConfig().withFamilyName("family").withGivenName("given").withName("name").build();

    assertThat(config.getFamilyName()).isEqualTo("family");
    assertThat(config.getGivenName()).isEqualTo("given");
    assertThat(config.getName()).isEqualTo("name");
  }

  @ParameterizedTest
  @MethodSource("givenName_familyName_and_expected_name")
  void name_is_filled_from_given_and_family_name_if_not_set(
      String givenName, String familyName, String expectedName) {
    TokenConfig config =
        TokenConfig.aTokenConfig().withGivenName(givenName).withFamilyName(familyName).build();

    assertThat(config.getName()).isEqualTo(expectedName);
  }

  private static Stream<Arguments> givenName_familyName_and_expected_name() {
    return Stream.of(
        Arguments.of("given", "family", "given family"),
        Arguments.of(null, "family", "family"),
        Arguments.of("given", null, "given"),
        Arguments.of(null, null, null));
  }

  @Test
  void hostname_is_set_correctly() {
    TokenConfig config = aTokenConfig().withHostname("hostname").build();

    assertThat(config.getHostname()).isEqualTo("hostname");
  }

  @Test
  void issuedAt_is_set_correctly() {
    Instant issuedAt = Instant.now().plusSeconds(5);
    TokenConfig config = aTokenConfig().withIssuedAt(issuedAt).build();

    assertThat(config.getIssuedAt()).isCloseTo(issuedAt, within(1, ChronoUnit.MILLIS));
  }

  @Test
  void notBefore_is_set_correctly() {
    Instant notBefore = Instant.now().plusSeconds(5);
    TokenConfig config = aTokenConfig().withNotBefore(notBefore).build();

    assertThat(config.getNotBefore()).isCloseTo(notBefore, within(1, ChronoUnit.MILLIS));
  }

  @Test
  void preferredUsername_is_set_correctly() {
    TokenConfig config = aTokenConfig().withPreferredUsername("preferred").build();

    assertThat(config.getPreferredUsername()).isEqualTo("preferred");
  }

  @Test
  void realm_is_set_correctly() {
    TokenConfig config = aTokenConfig().withRealm("realm").build();

    assertThat(config.getRealm()).isEqualTo("realm");
  }

  @Test
  void realmAccess_is_set_correctly() {
    TokenConfig config =
        aTokenConfig()
            .withRealmRole("role1")
            .withRealmRoles(Collections.singleton("role2"))
            .withRealmRole("role3")
            .build();

    assertThat(config.getRealmAccess().getRoles())
        .containsExactlyInAnyOrder("role1", "role2", "role3");
  }

  @Test
  void resourceAccess_is_set_correctly() {
    TokenConfig config =
        aTokenConfig()
            .withResourceRole("resource1", "role1_1")
            .withResourceRole("resource2", "role1_2")
            .withResourceRoles("resource1", Collections.singleton("role2_1"))
            .withResourceRole("resource1", "role3_1")
            .build();

    assertThat(config.getResourceAccess()).containsOnlyKeys("resource1", "resource2");
    assertThat(config.getResourceAccess().get("resource1").getRoles())
        .containsExactlyInAnyOrder("role1_1", "role2_1", "role3_1");
    assertThat(config.getResourceAccess().get("resource2").getRoles())
        .containsExactlyInAnyOrder("role1_2");
  }

  @Test
  void scope_is_set_correctly() {
    TokenConfig config =
        aTokenConfig()
            .withScope("scope1")
            .withScopes(Collections.singletonList("scope2"))
            .withScope("scope3")
            .build();

    assertThat(config.getScope().split(" ")).containsOnly("scope1", "scope2", "scope3");
  }

  @Test
  void subject_is_set_correctly() {
    TokenConfig config = aTokenConfig().withSubject("subject").build();

    assertThat(config.getSubject()).isEqualTo("subject");
  }

  @Test
  void user_data_is_generated_from_subject() {
    TokenConfig config = aTokenConfig().withSubjectAndGeneratedUserData("subject").build();

    assertThat(config.getSubject()).isEqualTo("subject");
    assertThat(config.isGenerateUserDataFromSubject()).isTrue();
  }

  @Test
  void config_is_set_correctly_from_original_token() {
    TokenConfig config = aTokenConfig().withSourceToken(KEYCLOAK_SOURCE_TOKEN).build();
    Instant now = Instant.now();

    assertThat(config.getAudience()).containsExactly("account");
    assertThat(config.getAuthenticationTime()).isBetween(now.minusSeconds(1), now);
    assertThat(config.getAuthorizedParty()).isEqualTo("client");
    assertThat(config.getClaims())
        .containsOnly(
            entry("jti", "9cfe2b60-1db9-4a12-b9dc-dcabfd16e945"),
            entry("nonce", "aef8cf1b-127d-4650-8081-0db24af2d0e7"),
            entry("session_state", "05270907-d7e2-4b75-8341-5bdd94eab763"),
            entry("allowed-origins", Collections.singletonList("http://localhost:3000")),
            entry("email_verified", false));
    assertThat(config.getEmail()).isEqualTo("user@keycloak");
    assertThat(config.getExpiration())
        .isCloseTo(now.plus(10, ChronoUnit.HOURS), within(1, ChronoUnit.SECONDS));
    assertThat(config.getFamilyName()).isEqualTo("User");
    assertThat(config.getGivenName()).isEqualTo("Peter");
    assertThat(config.getIssuedAt()).isBetween(now.minusSeconds(1), now);
    assertThat(config.getName()).isEqualTo("Peter User");
    assertThat(config.getNotBefore()).isNull();
    assertThat(config.getPreferredUsername()).isEqualTo("user");
    assertThat(config.getAuthenticationContextClassReference()).isEqualTo("1");
    assertThat(config.getRealmAccess().getRoles())
        .containsExactlyInAnyOrder("offline_access", "uma_authorization");
    assertThat(config.getResourceAccess()).containsOnlyKeys("account");
    assertThat(config.getResourceAccess().get("account").getRoles())
        .containsExactlyInAnyOrder("manage-account", "manage-account-links", "view-profile");
    assertThat(config.getScope().split(" "))
        .containsExactlyInAnyOrder("openid", "email", "profile");
    assertThat(config.getSubject()).isEqualTo("fbcaa40a-9480-4ead-adf2-8f6085f6f75d");
    assertThat(config.getHostname()).isEqualTo("localhost");
    assertThat(config.getRealm()).isEqualTo("realm");
  }

  @Test
  void invalid_issuer_causes_exception() {
    Builder builder = aTokenConfig();
    assertThatThrownBy(() -> builder.withSourceToken(TOKEN_WITH_INVALID_ISSUER_URL))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not a valid URL");
  }

  @Test
  void unexpected_issuer_causes_exception() {
    Builder builder = aTokenConfig();
    assertThatThrownBy(() -> builder.withSourceToken(TOKEN_WITH_UNEXPECTED_ISSUER_URL))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("did not conform to the expected format")
        .hasMessageContaining("'http[s]://$HOSTNAME[:port][:contextPath]/realms/$REALM'");
  }
}
