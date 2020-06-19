package com.tngtech.keycloakmock.junit5;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class KeycloakMockJunit5Test {
  private KeycloakMock keyCloakMock;

  @BeforeEach
  void setup() {
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    RestAssured.port = 8000;
  }

  @AfterEach
  void stopMock() {
    if (keyCloakMock != null) {
      keyCloakMock.afterAll(null);
    }
  }

  @Test
  void mock_is_running() {
    keyCloakMock = new KeycloakMock();
    keyCloakMock.beforeAll(null);

    RestAssured.when()
        .get("/auth/realms/master/protocol/openid-connect/certs")
        .then()
        .statusCode(200)
        .and()
        .contentType(ContentType.JSON);
  }

  @Test
  void https_is_working() {
    keyCloakMock = new KeycloakMock(8000, "master", true);
    keyCloakMock.beforeAll(null);

    RestAssured.given()
        .relaxedHTTPSValidation()
        .when()
        .get("https://localhost:8000/auth/realms/master/protocol/openid-connect/certs")
        .then()
        .statusCode(200);
  }
}
