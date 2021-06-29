package com.tngtech.keycloakmock.junit;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.matcher.ResponseAwareMatcher;
import io.restassured.response.Response;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class KeycloakMockRuleJunit4Test {

  @Rule public KeycloakMockRule keycloakMockRule = new KeycloakMockRule();

  @Before
  public void setup() {
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    RestAssured.port = 8000;
  }

  @Test
  public void mock_is_running() {
    RestAssured.when()
        .get("/auth/realms/master/protocol/openid-connect/certs")
        .then()
        .statusCode(200)
        .and()
        .contentType(ContentType.JSON);
  }

  @Test
  public void token_route_is_working() {
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
    keycloakMockRule
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
