package com.tngtech.keycloakmock.examplebackend;

import static com.tngtech.keycloakmock.api.ServerConfig.aServerConfig;
import static com.tngtech.keycloakmock.api.TokenConfig.aTokenConfig;
import static org.hamcrest.Matchers.equalTo;

import com.tngtech.keycloakmock.junit5.KeycloakMockExtension;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;

@SpringBootTest(
    classes = ExampleBackendApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthenticationTest {

  @RegisterExtension
  static KeycloakMockExtension mock =
      new KeycloakMockExtension(aServerConfig().withRealm("realm").build());

  @LocalServerPort private int port;

  @BeforeEach
  void setup() {
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    RestAssured.port = port;
  }

  @Test
  void no_authentication_fails() {
    RestAssured.given().when().get("/api/hello").then().statusCode(401);
  }

  @Test
  void authentication_works() {
    RestAssured.given()
        .auth()
        .preemptive()
        .oauth2(mock.getAccessToken(aTokenConfig().withSubject("Awesome").build()))
        .when()
        .get("/api/hello")
        .then()
        .statusCode(200)
        .and()
        .body(equalTo("Hello Awesome"));
  }

  @Test
  void authentication_without_role_fails() {
    RestAssured.given()
        .auth()
        .preemptive()
        .oauth2(mock.getAccessToken(aTokenConfig().build()))
        .when()
        .get("/api/vip")
        .then()
        .statusCode(403);
  }

  @Test
  void authentication_with_role_works() {
    RestAssured.given()
        .auth()
        .preemptive()
        .oauth2(mock.getAccessToken(aTokenConfig().withRealmRole("vip").build()))
        .when()
        .get("/api/vip")
        .then()
        .statusCode(200)
        .and()
        .body(equalTo("you may feel very special here"));
  }
}
