package com.tngtech.keycloakmock.api;

import static com.tngtech.keycloakmock.api.ServerConfig.aServerConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
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

  @Test
  void mock_server_fails_when_port_is_claimed() {
    KeycloakMock keycloakMock = new KeycloakMock();
    keycloakMock.start();
    KeycloakMock secondMock = new KeycloakMock();
    assertThatThrownBy(secondMock::start).isInstanceOf(MockServerException.class);
    keycloakMock.stop();
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
    KeycloakMock keycloakMock =
        new KeycloakMock(aServerConfig().withPort(port).withTls(tls).build());
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
  void generated_token_uses_correct_issuer() throws Exception {
    JwtParser jwtParser = Jwts.parserBuilder().setSigningKey(loadKey()).build();
    KeycloakMock keycloakMock =
        new KeycloakMock(
            aServerConfig().withPort(123).withDefaultRealm("realm123")
                .withDefaultHostname("somehost").build());

    String token = keycloakMock.getAccessToken(TokenConfig.aTokenConfig().build());

    Jws<Claims> jwt = jwtParser.parseClaimsJws(token);

    assertThat(jwt.getBody().getIssuer()).isEqualTo("http://somehost:123/auth/realms/realm123");
  }

  private RSAPublicKey loadKey() throws Exception {
    KeyStore keyStore = KeyStore.getInstance("JKS");
    try (InputStream keystoreStream = getClass().getResourceAsStream("/keystore.jks")) {
      keyStore.load(keystoreStream, null);
      return (RSAPublicKey) keyStore.getCertificate("rsa").getPublicKey();
    }
  }
}
