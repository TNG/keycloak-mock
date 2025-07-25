package com.tngtech.keycloakmock.api;

import static com.tngtech.keycloakmock.api.ServerConfig.aServerConfig;
import static com.tngtech.keycloakmock.test.KeyHelper.loadValidKey;
import static org.assertj.core.api.Assertions.assertThat;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.assertj.core.util.Sets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class KeycloakMockTest {
  private static final String DEFAULT_HOST = "defaultHost";
  private static final String DEFAULT_REALM = "defaultRealm";
  private static final String TOKEN_HOST = "tokenHost";
  private static final String TOKEN_REALM = "tokenRealm";
  private static final ServerConfig SERVER_CONFIG =
      aServerConfig().withDefaultHostname(DEFAULT_HOST).withDefaultRealm(DEFAULT_REALM).build();

  private final JwtParser jwtParser = Jwts.parser().verifyWith(loadValidKey()).build();

  static Stream<Arguments> token_host_and_realm_and_expected_issuer() {
    return Stream.of(
        Arguments.of(null, null, "http://defaultHost:8000/auth/realms/defaultRealm"),
        Arguments.of(TOKEN_HOST, null, "http://tokenHost/auth/realms/defaultRealm"),
        Arguments.of(null, TOKEN_REALM, "http://defaultHost:8000/auth/realms/tokenRealm"),
        Arguments.of(TOKEN_HOST, TOKEN_REALM, "http://tokenHost/auth/realms/tokenRealm"));
  }

  @ParameterizedTest
  @MethodSource("token_host_and_realm_and_expected_issuer")
  void generated_token_uses_correct_issuer(
      @Nullable String host, @Nullable String realm, @Nonnull String expectedIssuer) {
    KeycloakMock keycloakMock = new KeycloakMock(SERVER_CONFIG);

    TokenConfig.Builder builder = TokenConfig.aTokenConfig();
    if (host != null) {
      builder.withHostname(host);
    }
    if (realm != null) {
      builder.withRealm(realm);
    }

    String token = keycloakMock.getAccessToken(builder.build());

    Jws<Claims> jwt = jwtParser.parseSignedClaims(token);

    assertThat(jwt.getPayload().getIssuer()).isEqualTo(expectedIssuer);
  }

  @Test
  void contains_client_scopes_during_server_configuration() {
    Set<String> scopes = Sets.set("TestScope1", "TestScope2");
    KeycloakMock keycloakMock = new KeycloakMock(aServerConfig().withDefaultScopes(scopes).build());

    String token = keycloakMock.getAccessToken(TokenConfig.aTokenConfig().build());

    Jws<Claims> jwt = jwtParser.parseSignedClaims(token);

    assertThat(jwt.getPayload()).containsEntry("scope", "openid TestScope1 TestScope2");
  }

  @Test
  void contains_default_client_scope_during_server_configuration() {
    KeycloakMock keycloakMock = new KeycloakMock(aServerConfig().build());

    String token = keycloakMock.getAccessToken(TokenConfig.aTokenConfig().build());

    Jws<Claims> jwt = jwtParser.parseSignedClaims(token);

    assertThat(jwt.getPayload()).containsEntry("scope", "openid");
  }

  @Test
  void contains_default_audiences() {
    Set<String> resources = Sets.set("audience1", "audience2");
    KeycloakMock keycloakMock =
        new KeycloakMock(aServerConfig().withDefaultAudiences(resources).build());

    String token = keycloakMock.getAccessToken(TokenConfig.aTokenConfig().build());

    Jws<Claims> jwt = jwtParser.parseSignedClaims(token);

    assertThat(jwt.getPayload().getAudience()).containsExactlyInAnyOrder("audience1", "audience2");
  }
}
