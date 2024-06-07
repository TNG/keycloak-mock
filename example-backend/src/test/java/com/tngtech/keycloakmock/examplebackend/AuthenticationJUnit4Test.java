package com.tngtech.keycloakmock.examplebackend;

import static com.tngtech.keycloakmock.api.ServerConfig.aServerConfig;
import static com.tngtech.keycloakmock.api.TokenConfig.aTokenConfig;
import static org.hamcrest.Matchers.equalTo;

import com.tngtech.keycloakmock.junit.KeycloakMockRule;
import io.restassured.RestAssured;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(
    classes = ExampleBackendApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AuthenticationJUnit4Test {

  @ClassRule
  public static KeycloakMockRule mock =
      new KeycloakMockRule(aServerConfig().withDefaultRealm("realm").build());

  @LocalServerPort private int port;

  @Before
  public void setup() {
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    RestAssured.port = port;
  }

  @Test
  public void no_authentication_fails() {
    RestAssured.given().when().get("/api/hello").then().statusCode(401);
  }

  @Test
  public void authentication_works() {
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
  public void authentication_without_role_fails() {
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
  public void authentication_with_role_works() {
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

  @Test
  public void authentication_with_resource_role_works() {
    RestAssured.given()
        .auth()
        .preemptive()
        .oauth2(mock.getAccessToken(aTokenConfig().withResourceRole("server", "vip").build()))
        .when()
        .get("/api/vip")
        .then()
        .statusCode(200)
        .and()
        .body(equalTo("you may feel very special here"));
  }
}
