package com.tngtech.keycloakmock.junit5;

import static com.tngtech.keycloakmock.api.ServerConfig.aServerConfig;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.matcher.ResponseAwareMatcher;
import io.restassured.response.Response;

class KeycloakMockExtensionJunit5Test {
  private KeycloakMockExtension keyCloakMockExtension;

  @BeforeEach
  void setup() {
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    RestAssured.port = 8000;
  }

  @AfterEach
  void stopMock() {
    if (keyCloakMockExtension != null) {
      keyCloakMockExtension.afterAll(null);
    }
  }

  @Test
  void mock_is_running() {
    keyCloakMockExtension = new KeycloakMockExtension();
    keyCloakMockExtension.beforeAll(null);

    RestAssured.when()
        .get("/auth/realms/master/protocol/openid-connect/certs")
        .then()
        .statusCode(200)
        .and()
        .contentType(ContentType.JSON);
  }

  @Test
  void https_is_working() {
    keyCloakMockExtension = new KeycloakMockExtension(aServerConfig().withTls(true).build());
    keyCloakMockExtension.beforeAll(null);

    RestAssured.given()
        .relaxedHTTPSValidation()
        .when()
        .get("https://localhost:8000/auth/realms/master/protocol/openid-connect/certs")
        .then()
        .statusCode(200);
  }

  @Test
  void token_route_is_working() {
    keyCloakMockExtension = new KeycloakMockExtension();
    keyCloakMockExtension.beforeAll(null);

    RestAssured.when()
        .post("/auth/realms/master/protocol/openid-connect/token")
        .then()
        .statusCode(200)
        .and()
        .contentType(ContentType.JSON)
        .assertThat()
        .body(
            "access_token",
            (ResponseAwareMatcher<Response>) response -> Matchers.blankOrNullString())
        .body(
            "refresh_token",
            (ResponseAwareMatcher<Response>) response -> Matchers.blankOrNullString())
        .body("id_token", (ResponseAwareMatcher<Response>) response -> Matchers.blankOrNullString())
        .body(
            "token_type",
            (ResponseAwareMatcher<Response>) response -> Matchers.hasToString("Bearer"))
        .body("expires_in", (ResponseAwareMatcher<Response>) response -> Matchers.equalTo(3600));

    // test error response in same mock
    keyCloakMockExtension
        .getTokenRoute()
        .withErrorResponse(404, "{\"error\": \"Error detail message\"}");
    RestAssured.when()
        .post("/auth/realms/master/protocol/openid-connect/token")
        .then()
        .statusCode(404)
        .and()
        .contentType(ContentType.JSON);
  }
}
