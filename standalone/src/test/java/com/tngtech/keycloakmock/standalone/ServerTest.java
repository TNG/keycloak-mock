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
import io.restassured.path.json.JsonPath;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
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

  private static final String LOGIN_PAGE_URL_TEMPLATE =
      "http://localhost:8001/auth/realms/realm/protocol/openid-connect/auth?client_id=client&redirect_uri=%s&state=%s&nonce=%s&response_type=%s";
  private static final String TOKEN_ENDPOINT_URL =
      "http://localhost:8001/auth/realms/realm/protocol/openid-connect/token";
  private static JwtParser jwtParser;
  private Server server = null;

  @BeforeAll
  static void setupJwtsParser() throws Exception {
    jwtParser = Jwts.parserBuilder().setSigningKey(loadKey()).build();
  }

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
    server = new Server(8001, false, Collections.emptyList());

    // open login page to create session (implicit flow)
    ClientRequest firstRequest = new ClientRequest("redirect-uri", "state", "nonce", "id_token");
    String callbackUrl = openLoginPageAndGetCallbackUrl(firstRequest);

    // simulate login
    Cookie keycloakSession = loginAndValidateAndReturnSessionCookie(firstRequest, callbackUrl);

    // subsequent request to login page with session cookie will immediately return
    ClientRequest secondRequest =
        new ClientRequest("redirect-uri2", "state2", "nonce2", "id_token");
    openLoginPageAgainAndExpectToBeLoggedInAlready(secondRequest, keycloakSession);

    // logout
    logoutAndExpectSessionCookieReset();
  }

  @Test
  void mock_server_login_with_authorization_code_flow_works() throws Exception {
    server = new Server(8001, false, Collections.emptyList());

    // open login page to create session (authorization code flow)
    ClientRequest firstRequest = new ClientRequest("redirect-uri", "state", "nonce", "code");
    String callbackUrl = openLoginPageAndGetCallbackUrl(firstRequest);

    // simulate login
    String authorizationCode = loginAndValidateAndReturnAuthCode(firstRequest, callbackUrl);
    String refreshToken =
        validateAuthorizationAndRetrieveToken(authorizationCode, firstRequest.getNonce());

    // refresh token flow
    validateRefreshTokenFlow(refreshToken, firstRequest.getNonce());

    // logout
    logoutAndExpectSessionCookieReset();
  }

  private String openLoginPageAndGetCallbackUrl(ClientRequest request) {
    return RestAssured.given()
        .when()
        .get(request.getLoginPageUrl())
        .then()
        .assertThat()
        .statusCode(200)
        .extract()
        .htmlPath()
        .getNode("html")
        .getNode("body")
        .getNode("form")
        .getAttribute("action");
  }

  private Cookie loginAndValidateAndReturnSessionCookie(ClientRequest request, String callbackUrl)
      throws URISyntaxException {
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

    validateProperlyLoggedInAndRedirected(request, keycloakSession, location);
    return keycloakSession;
  }

  private String loginAndValidateAndReturnAuthCode(ClientRequest request, String callbackUrl)
      throws URISyntaxException {
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

    return validateProperlyRedirectedWithAuthorizationCode(request, keycloakSession, location);
  }

  private String validateAuthorizationAndRetrieveToken(String authorizationCode, String nonce) {
    ExtractableResponse<Response> extractableResponse =
        RestAssured.given()
            .config(config().redirect(redirectConfig().followRedirects(false)))
            .when()
            .formParam("grant_type", "authorization_code")
            .formParam("code", authorizationCode)
            .post(TOKEN_ENDPOINT_URL)
            .then()
            .assertThat()
            .statusCode(200)
            .extract();
    JsonPath body = extractableResponse.body().jsonPath();
    String accessToken = body.getString("access_token");

    validateToken(accessToken, nonce);

    return body.getString("refresh_token");
  }

  private void validateRefreshTokenFlow(String refreshToken, String nonce) {
    ExtractableResponse<Response> extractableResponse =
        RestAssured.given()
            .config(config().redirect(redirectConfig().followRedirects(false)))
            .when()
            .formParam("grant_type", "refresh_token")
            .formParam("refresh_token", refreshToken)
            .post(TOKEN_ENDPOINT_URL)
            .then()
            .assertThat()
            .statusCode(200)
            .extract();
    String accessToken = extractableResponse.body().jsonPath().getString("access_token");

    validateToken(accessToken, nonce);
  }

  private void validateToken(String accessToken, String nonce) {
    Jws<Claims> jwt = jwtParser.parseClaimsJws(accessToken);
    assertThat(jwt.getBody().getIssuer()).isEqualTo("http://localhost:8001/auth/realms/realm");
    TokenConfig tokenConfig = TokenConfig.aTokenConfig().withSourceToken(accessToken).build();
    assertThat(tokenConfig.getPreferredUsername()).isEqualTo("username");
    assertThat(tokenConfig.getRealmAccess().getRoles())
        .containsExactlyInAnyOrder("role1", "role2", "role3");
    assertThat(tokenConfig.getClaims()).containsEntry("nonce", nonce);
  }

  private void openLoginPageAgainAndExpectToBeLoggedInAlready(
      ClientRequest request, Cookie keycloakSession) throws URISyntaxException {
    String location =
        RestAssured.given()
            .config(config().redirect(redirectConfig().followRedirects(false)))
            .when()
            .cookie(keycloakSession)
            .get(request.getLoginPageUrl())
            .then()
            .assertThat()
            .statusCode(302)
            .extract()
            .header("location");

    validateProperlyLoggedInAndRedirected(request, keycloakSession, location);
  }

  private void validateProperlyLoggedInAndRedirected(
      ClientRequest request, Cookie keycloakSession, String location) throws URISyntaxException {
    String sessionId = validateCookieAndReturnSessionId(keycloakSession);

    assertThat(location).contains("#").doesNotContain("?");
    // neither Java nor AssertJ currently support extracting parameters from a fragment, so use a
    // query instead
    URI redirectUri = new URI("http://" + location.replace('#', '?'));
    assertThat(redirectUri)
        .as("redirect URL is correctly set")
        .hasHost(request.getRedirectUri())
        .hasParameter("state", request.getState())
        .hasParameter("session_state", sessionId);

    // there is no way to extract the value of id_token directly, so use string manipulation
    assertThat(location).matches(".*[#&]id_token=[^#&?]+");
    String token = location.split("id_token=")[1];
    validateToken(token, request.getNonce());
  }

  private String validateProperlyRedirectedWithAuthorizationCode(
      ClientRequest request, Cookie keycloakSession, String location) throws URISyntaxException {
    String sessionId = validateCookieAndReturnSessionId(keycloakSession);

    assertThat(location).contains("?").doesNotContain("#");
    URI redirectUri = new URI("http://" + location);
    assertThat(redirectUri)
        .as("redirect URL is correctly set")
        .hasHost(request.getRedirectUri())
        .hasParameter("state", request.getState())
        .hasParameter("session_state", sessionId);

    // there is no way to extract the value of authorization_code directly, so use string
    // manipulation
    assertThat(location).matches(".*[?&]code=[^?&]+");
    return location.split("code=")[1];
  }

  private String validateCookieAndReturnSessionId(Cookie keycloakSession) {
    assertThat(keycloakSession.getPath()).isEqualTo("/auth/realms/realm/");
    assertThat(keycloakSession.getMaxAge()).isEqualTo(36000);
    String[] components = keycloakSession.getValue().split("/");
    assertThat(components).hasSize(3);
    assertThat(components[0]).isEqualTo("realm");
    return components[2];
  }

  private void logoutAndExpectSessionCookieReset() {
    RestAssured.given()
        .config(config().redirect(redirectConfig().followRedirects(false)))
        .when()
        .get(
            "http://localhost:8001/auth/realms/realm/protocol/openid-connect/logout?redirect_uri=redirect_uri")
        .then()
        .assertThat()
        .statusCode(302)
        .header("location", "redirect_uri")
        .cookie("KEYCLOAK_SESSION", "realm/dummy-user-id");
  }

  private static class ClientRequest {

    private final String redirectUri;
    private final String state;
    private final String nonce;
    private final String responseType;

    private ClientRequest(String redirectUri, String state, String nonce, String responseType) {
      this.redirectUri = redirectUri;
      this.state = state;
      this.nonce = nonce;
      this.responseType = responseType;
    }

    public String getRedirectUri() {
      return redirectUri;
    }

    public String getState() {
      return state;
    }

    public String getNonce() {
      return nonce;
    }

    public String getLoginPageUrl() {
      return String.format(LOGIN_PAGE_URL_TEMPLATE, redirectUri, state, nonce, responseType);
    }
  }

  private static RSAPublicKey loadKey() throws Exception {
    KeyStore keyStore = KeyStore.getInstance("JKS");
    try (InputStream keystoreStream = ServerTest.class.getResourceAsStream("/keystore.jks")) {
      keyStore.load(keystoreStream, null);
      return (RSAPublicKey) keyStore.getCertificate("rsa").getPublicKey();
    }
  }
}
