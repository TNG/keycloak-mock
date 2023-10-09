package com.tngtech.keycloakmock.api;

import static com.tngtech.keycloakmock.api.ServerConfig.aServerConfig;
import static com.tngtech.keycloakmock.test.KeyHelper.loadValidKey;
import static org.assertj.core.api.Assertions.assertThat;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import java.util.Set;
import org.assertj.core.util.Sets;
import org.junit.jupiter.api.Test;

class KeycloakMockTest {
  private final JwtParser jwtParser = Jwts.parser().verifyWith(loadValidKey()).build();

  @Test
  void generated_token_uses_correct_issuer() {
    KeycloakMock keycloakMock =
        new KeycloakMock(
            aServerConfig()
                .withPort(123)
                .withDefaultRealm("realm123")
                .withDefaultHostname("somehost")
                .build());

    String token = keycloakMock.getAccessToken(TokenConfig.aTokenConfig().build());

    Jws<Claims> jwt = jwtParser.parseSignedClaims(token);

    assertThat(jwt.getPayload().getIssuer()).isEqualTo("http://somehost:123/auth/realms/realm123");
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
}
