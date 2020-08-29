package com.tngtech.keycloakmock.api;

import static org.assertj.core.api.Assertions.assertThat;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.interfaces.RSAPublicKey;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class KeycloakMockTest {

  @Test
  void mock_server_can_be_started_and_stopped() {
    KeycloakMock keycloakMock = new KeycloakMock();
    assertServerMockRunnning(false);
    keycloakMock.start();
    assertServerMockRunnning(true);
    keycloakMock.stop();
    assertServerMockRunnning(false);
  }

  private void assertServerMockRunnning(boolean running) {
    try {
      RestAssured.when()
          .get("http://localhost:8000/auth/realms/master/protocol/openid-connect/certs")
          .then()
          .statusCode(200)
          .and()
          .contentType(ContentType.JSON);
      assertThat(running).as("Exception should occur if server is not running").isTrue();
    } catch (Throwable e) {
      assertThat(running).as("Exception should occur if server is not running").isFalse();
    }
  }

  @ParameterizedTest
  @MethodSource("serverConfig")
  void mock_server_endpoint_is_correctly_configured(int port, boolean tls) {
    KeycloakMock keycloakMock = new KeycloakMock(port, "master", tls);
    keycloakMock.start();
    RestAssured.given()
        .relaxedHTTPSValidation()
        .when()
        .get(
            (tls ? "https" : "http")
                + "://localhost:"
                + port
                + "/auth/realms/master/protocol/openid-connect/certs")
        .then()
        .statusCode(200)
        .and()
        .contentType(ContentType.JSON);
    keycloakMock.stop();
  }

  private static Stream<Arguments> serverConfig() {
    return Stream.of(Arguments.of(8000, false), Arguments.of(8001, true));
  }

  @Test
  @SuppressWarnings("unchecked")
  void generated_token_uses_correct_issuer() throws Exception {
    JwtParser jwtParser = Jwts.parserBuilder().setSigningKey(loadKey()).build();
    KeycloakMock keycloakMock = new KeycloakMock(123, "realm123");

    String token = keycloakMock.getAccessToken(TokenConfig.aTokenConfig().build());

    Jwt<Header<?>, Claims> jwt = jwtParser.parse(token);

    assertThat(jwt.getBody().getIssuer()).isEqualTo("http://localhost:123/auth/realms/realm123");

    token =
        keycloakMock.getAccessTokenForHostnameAndRealm(
            TokenConfig.aTokenConfig().build(), "https://remotehost:321", "another_realm");

    jwt = jwtParser.parse(token);

    assertThat(jwt.getBody().getIssuer())
        .isEqualTo("https://remotehost:321/auth/realms/another_realm");
  }

  private RSAPublicKey loadKey() throws Exception {
    KeyStore keyStore = KeyStore.getInstance("JKS");
    try (InputStream keystoreStream =
        TokenGeneratorTest.class.getResourceAsStream("/keystore.jks")) {
      keyStore.load(keystoreStream, null);
      return (RSAPublicKey) keyStore.getCertificate("rsa").getPublicKey();
    }
  }
}
