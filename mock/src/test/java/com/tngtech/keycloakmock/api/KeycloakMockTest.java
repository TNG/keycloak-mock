package com.tngtech.keycloakmock.api;

import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
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
}
