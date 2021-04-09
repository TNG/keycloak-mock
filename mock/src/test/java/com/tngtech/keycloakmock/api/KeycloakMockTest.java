package com.tngtech.keycloakmock.api;

import static com.tngtech.keycloakmock.api.ServerConfig.aServerConfig;
import static com.tngtech.keycloakmock.test.KeyHelper.loadValidKey;
import static org.assertj.core.api.Assertions.assertThat;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;

class KeycloakMockTest {

  @Test
  void generated_token_uses_correct_issuer() throws Exception {
    JwtParser jwtParser = Jwts.parserBuilder().setSigningKey(loadValidKey()).build();
    KeycloakMock keycloakMock =
        new KeycloakMock(
            aServerConfig()
                .withPort(123)
                .withDefaultRealm("realm123")
                .withDefaultHostname("somehost")
                .build());

    String token = keycloakMock.getAccessToken(TokenConfig.aTokenConfig().build());

    Jws<Claims> jwt = jwtParser.parseClaimsJws(token);

    assertThat(jwt.getBody().getIssuer()).isEqualTo("http://somehost:123/auth/realms/realm123");
  }
}
