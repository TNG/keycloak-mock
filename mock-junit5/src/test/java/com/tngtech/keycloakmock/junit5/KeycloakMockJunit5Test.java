package com.tngtech.keycloakmock.junit5;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import org.hamcrest.Matchers;
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
    keyCloakMock = new KeycloakMock(8000, "master");
    keyCloakMock.beforeAll(null);

    JsonPath expected =
        JsonPath.from(
            "{\n"
                + "  \"keys\" : [ {\n"
                + "    \"kid\" : \"keyId\",\n"
                + "    \"kty\" : \"RSA\",\n"
                + "    \"alg\" : \"RS256\",\n"
                + "    \"use\" : \"sig\",\n"
                + "    \"n\" : \"AKzaf4nijuwtAn9ieZaz+iGXBp1pFm6dJMAxRO6ax2CV9cBFeThxrKJNFmDY7j7gKRnrgWxvgJKSd3hAm/CGmXHbTM8cPi/gsof+CsOohv7LH0UYbr0UpCIJncTiRrKQto7q/NOO4Jh1EBSLMPX7MzttEhh35Ue9txHLq3zkdkR6BR6nGS7QxEg7FzYzA4IooV59OPr+TvlDxbEpwc1wkRZDGavo+WjngAt7m/BEQtHnav3whitbrMmi/1tWY8cQbO9D4FuQTM7yvACLSv94G2TCvsjm/gGJmOJyRBkI1r+uEIfhz9+VIKlswqapKSul+Hoxv5NycucRa4xi4N39dfM=\",\n"
                + "    \"e\" : \"AQAB\"\n"
                + "  } ]\n"
                + "}");

    RestAssured.given()
        .when()
        .get("/auth/realms/master/protocol/openid-connect/certs")
        .then()
        .statusCode(200)
        .and()
        .contentType(ContentType.JSON)
        .and()
        .body("", Matchers.equalTo(expected.getMap("")));
  }

  @Test
  void https_is_working() {
    keyCloakMock = new KeycloakMock(8000, "master", true);
    keyCloakMock.beforeAll(null);
    JsonPath expected =
        JsonPath.from(
            "{\n"
                + "  \"keys\" : [ {\n"
                + "    \"kid\" : \"keyId\",\n"
                + "    \"kty\" : \"RSA\",\n"
                + "    \"alg\" : \"RS256\",\n"
                + "    \"use\" : \"sig\",\n"
                + "    \"n\" : \"AKzaf4nijuwtAn9ieZaz+iGXBp1pFm6dJMAxRO6ax2CV9cBFeThxrKJNFmDY7j7gKRnrgWxvgJKSd3hAm/CGmXHbTM8cPi/gsof+CsOohv7LH0UYbr0UpCIJncTiRrKQto7q/NOO4Jh1EBSLMPX7MzttEhh35Ue9txHLq3zkdkR6BR6nGS7QxEg7FzYzA4IooV59OPr+TvlDxbEpwc1wkRZDGavo+WjngAt7m/BEQtHnav3whitbrMmi/1tWY8cQbO9D4FuQTM7yvACLSv94G2TCvsjm/gGJmOJyRBkI1r+uEIfhz9+VIKlswqapKSul+Hoxv5NycucRa4xi4N39dfM=\",\n"
                + "    \"e\" : \"AQAB\"\n"
                + "  } ]\n"
                + "}");

    RestAssured.given()
        .relaxedHTTPSValidation()
        .when()
        .get("https://localhost:8000/auth/realms/master/protocol/openid-connect/certs")
        .then()
        .statusCode(200)
        .and()
        .body("", Matchers.equalTo(expected.getMap("")));
  }
}
