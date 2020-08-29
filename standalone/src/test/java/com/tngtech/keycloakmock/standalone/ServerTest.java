package com.tngtech.keycloakmock.standalone;

import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ServerTest {
  @BeforeAll
  static void setupRestAssured() {
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
  }

  @ParameterizedTest
  @MethodSource("serverConfig")
  void mock_server_endpoint_is_correctly_configured(int port, boolean tls) {
    Server server = new Server(port, tls);
    RestAssured.given()
        .relaxedHTTPSValidation()
        .when()
        .get(
            (tls ? "https" : "http")
                + "://localhost:"
                + port
                + "/auth/realms/master/protocol/openid-connect/certs")
        .then()
        .assertThat()
        .statusCode(200)
        .and()
        .contentType(ContentType.JSON);
    server.stop();
  }

  private static Stream<Arguments> serverConfig() {
    return Stream.of(Arguments.of(8000, false), Arguments.of(8001, true));
  }

  @Test
  void mock_server_uses_host_header_as_server_host() {
    String hostname = "server";
    Server server = new Server(8001, false);
    String issuer = RestAssured.given()
        .relaxedHTTPSValidation()
        .when()
        .header("Host", hostname)
        .get(
            "http://localhost:8001/auth/realms/test/.well-known/openid-configuration")
        .then()
        .assertThat()
        .statusCode(200)
        .and()
        .extract().jsonPath()
        .get("issuer");

    assertThat(issuer).isEqualTo("http://%s/auth/realms/test", hostname);
    server.stop();
  }
}
