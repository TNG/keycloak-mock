package com.tngtech.keycloakmock.standalone;

import static io.restassured.config.RedirectConfig.redirectConfig;
import static io.restassured.config.RestAssuredConfig.config;
import static io.restassured.http.ContentType.HTML;
import static io.restassured.http.ContentType.JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.params.provider.Arguments.of;

import com.tngtech.keycloakmock.api.TokenConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Cookie;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.interfaces.RSAPublicKey;
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

  @Test
  void mock_server_login_works() throws Exception {
    JwtParser jwtParser = Jwts.parserBuilder().setSigningKey(loadKey()).build();
    server = new Server(8001, false, Collections.emptyList());

    // open login page to create session (implicit flow)
    String callbackUrl =
        RestAssured.given()
            .when()
            .get(
                "http://localhost:8001/auth/realms/realm/protocol/openid-connect/auth?client_id=client&state=state&nonce=nonce&redirect_uri=redirect_uri&response_type=id_token")
            .then()
            .assertThat()
            .statusCode(200)
            .extract()
            .htmlPath()
            .getNode("html")
            .getNode("body")
            .getNode("form")
            .getAttribute("action");

    // simulate login POST and get id_token in location response header
    ExtractableResponse<Response> extractableResponse =
        RestAssured.given()
            .config(config().redirect(redirectConfig().followRedirects(false)))
            .when()
            .formParam("username", "username")
            .formParam("password", "role1,role2,role3")
            .post(callbackUrl)
            .then()
            .assertThat()
            .statusCode(302)
            .extract();
    String location = extractableResponse.header("location");
    Cookie keycloakSession = extractableResponse.detailedCookie("KEYCLOAK_SESSION");

    assertThat(location).contains("redirect_uri#state=state", "&session_state=", "id_token=");

    String token = location.split("id_token=")[1];

    Jws<Claims> jwt = jwtParser.parseClaimsJws(token);

    assertThat(jwt.getBody().getIssuer()).isEqualTo("http://localhost:8001/auth/realms/realm");

    TokenConfig tokenConfig = TokenConfig.aTokenConfig().withSourceToken(token).build();

    assertThat(tokenConfig.getPreferredUsername()).isEqualTo("username");
    assertThat(tokenConfig.getRealmAccess().getRoles())
        .containsExactlyInAnyOrder("role1", "role2", "role3");
    assertThat(tokenConfig.getClaims()).containsEntry("nonce", "nonce");

    // subsequent request to login page with session cookie set will immediately return
    String locationFromSession =
        RestAssured.given()
            .config(config().redirect(redirectConfig().followRedirects(false)))
            .when()
            .cookie(keycloakSession)
            .get(
                "http://localhost:8001/auth/realms/realm/protocol/openid-connect/auth?client_id=client2&state=state2&nonce=nonce2&redirect_uri=redirect_uri2&response_type=id_token")
            .then()
            .assertThat()
            .statusCode(302)
            .extract()
            .header("location");

    assertThat(locationFromSession)
        .contains("redirect_uri2#state=state2", "&session_state=", "id_token=");

    String token2 = locationFromSession.split("id_token=")[1];

    Jws<Claims> jwt2 = jwtParser.parseClaimsJws(token2);

    assertThat(jwt2.getBody().getIssuer()).isEqualTo("http://localhost:8001/auth/realms/realm");

    TokenConfig tokenConfig2 = TokenConfig.aTokenConfig().withSourceToken(token2).build();

    assertThat(tokenConfig2.getPreferredUsername()).isEqualTo("username");
    assertThat(tokenConfig2.getRealmAccess().getRoles())
        .containsExactlyInAnyOrder("role1", "role2", "role3");
    assertThat(tokenConfig2.getClaims()).containsEntry("nonce", "nonce2");

    // logout
    RestAssured.given()
        .config(config().redirect(redirectConfig().followRedirects(false)))
        .when()
        .get(
            "http://localhost:8001/auth/realms/realm/protocol/openid-connect/logout?redirect_uri=redirect_uri")
        .then()
        .assertThat()
        .statusCode(302)
        .header("location", "redirect_uri");
  }

  private RSAPublicKey loadKey() throws Exception {
    KeyStore keyStore = KeyStore.getInstance("JKS");
    try (InputStream keystoreStream = getClass().getResourceAsStream("/keystore.jks")) {
      keyStore.load(keystoreStream, null);
      return (RSAPublicKey) keyStore.getCertificate("rsa").getPublicKey();
    }
  }
}
