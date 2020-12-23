package com.tngtech.keycloakmock.standalone;

import static io.restassured.http.ContentType.HTML;
import static io.restassured.http.ContentType.JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.params.provider.Arguments.of;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.util.Collections;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ServerTest {

  private Server server = null;

  @BeforeAll
  static void setupRestAssured() {
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
  }

  @AfterEach
  void stopServer() {
    if (server != null) {
      server.stop();
    }
  }

  @ParameterizedTest
  @MethodSource("serverConfig")
  void mock_server_endpoint_is_correctly_configured(int port, boolean tls) {
    server = new Server(port, tls, Collections.emptyList());
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
        .contentType(JSON);
  }

  private static Stream<Arguments> serverConfig() {
    return Stream.of(of(8000, false), of(8001, true));
  }

  @Test
  void mock_server_uses_host_header_as_server_host() {
    String hostname = "server";
    server = new Server(8001, false, Collections.emptyList());
    String issuer =
        RestAssured.given()
            .when()
            .header("Host", hostname)
            .get("http://localhost:8001/auth/realms/test/.well-known/openid-configuration")
            .then()
            .assertThat()
            .statusCode(200)
            .and()
            .extract()
            .jsonPath()
            .get("issuer");

    assertThat(issuer).isEqualTo("http://%s/auth/realms/test", hostname);
  }

  private static Stream<Arguments> resourcesWithContent() {
    return Stream.of(
        of("/realms/test/protocol/openid-connect/login-status-iframe.html", HTML, "getCookie()"),
        of("/realms/test/protocol/openid-connect/3p-cookies/step1.html", HTML, "step2.html"),
        of("/realms/test/protocol/openid-connect/3p-cookies/step2.html", HTML, "\"supported\""),
        of("/js/keycloak.js", JSON, "var Keycloak"));
  }

  @Test
  void mock_server_answers_204_on_iframe_init() {
    server = new Server(8001, false, Collections.emptyList());
    RestAssured.given()
        .when()
        .get(
            "http://localhost:8001/auth/realms/test/protocol/openid-connect/login-status-iframe.html/init")
        .then()
        .assertThat()
        .statusCode(204)
        .and()
        .body(is(emptyString()));
  }

  @ParameterizedTest
  @MethodSource("resourcesWithContent")
  void mock_server_properly_returns_resources(
      String resource, ContentType contentType, String content) {
    server = new Server(8001, false, Collections.emptyList());
    String body =
        RestAssured.given()
            .when()
            .get("http://localhost:8001/auth" + resource)
            .then()
            .assertThat()
            .statusCode(200)
            .and()
            .contentType(contentType)
            .extract()
            .body()
            .asString();

    assertThat(body).contains(content);
  }

  @Test
  void mock_server_returns_404_on_nonexistent_resource() {
    server = new Server(8001, false, Collections.emptyList());
    RestAssured.given()
        .when()
        .get("http://localhost:8001/i-do-not-exist")
        .then()
        .assertThat()
        .statusCode(404);
  }
}
