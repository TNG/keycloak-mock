package com.tngtech.keycloakmock.junit;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.tngtech.keycloakmock.api.ServerConfig.aServerConfig;

public class KeycloakMockRuleJunit4Test {

  @Rule public KeycloakMockRule keycloakMockRule = new KeycloakMockRule();

  @Rule public KeycloakMockRule randomPortKeycloakMockRule = new KeycloakMockRule(aServerConfig().withPort(0).build());

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
  public void mock_is_running_on_a_random_port() {
      RestAssured.port = randomPortKeycloakMockRule.getActualPort();

      RestAssured.when()
          .get("/auth/realms/master/protocol/openid-connect/certs")
          .then()
          .statusCode(200)
          .and()
          .contentType(ContentType.JSON);
  }
}
