package com.tngtech.keycloakmock.api;

import static com.tngtech.keycloakmock.api.ServerConfig.aServerConfig;
import static com.tngtech.keycloakmock.test.KeyHelper.loadValidKey;
import static io.restassured.RestAssured.enableLoggingOfRequestAndResponseIfValidationFails;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static io.restassured.config.RedirectConfig.redirectConfig;
import static io.restassured.config.RestAssuredConfig.config;
import static io.restassured.http.ContentType.HTML;
import static io.restassured.http.ContentType.JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.params.provider.Arguments.of;

import com.tngtech.keycloakmock.impl.UrlConfiguration;
import com.tngtech.keycloakmock.impl.handler.IFrameRoute;
import com.tngtech.keycloakmock.test.ConfigurationResponse;
import io.fusionauth.jwks.JSONWebKeySetHelper;
import io.fusionauth.jwks.domain.JSONWebKey;
import io.fusionauth.jwt.Verifier;
import io.fusionauth.jwt.domain.JWT;
import io.fusionauth.jwt.ec.ECVerifier;
import io.fusionauth.jwt.rsa.RSAVerifier;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.restassured.http.ContentType;
import io.restassured.http.Cookie;
import io.restassured.http.Method;
import io.restassured.path.json.JsonPath;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class KeycloakMockIntegrationTest {

  private static final String LOGIN_PAGE_URL_TEMPLATE =
      "http://localhost:8000/auth/realms/realm/protocol/openid-connect/auth?client_id=client&redirect_uri=%s&state=%s&nonce=%s&response_type=%s";
  private static final String TOKEN_ENDPOINT_URL =
      "http://localhost:8000/auth/realms/realm/protocol/openid-connect/token";
  private static JwtParser jwtParser;
  private KeycloakMock keycloakMock = null;

  @BeforeAll
  static void setupJwtsParser() {
    jwtParser = Jwts.parser().verifyWith(loadValidKey()).build();
  }

  @BeforeAll
  static void setupRestAssured() {
    enableLoggingOfRequestAndResponseIfValidationFails();
  }

  @AfterEach
  void tearDown() {
    if (keycloakMock != null) {
      keycloakMock.stop();
    }
  }

  @Test
  void mock_server_can_be_started_and_stopped() {
    keycloakMock = new KeycloakMock();
    assertServerMockRunnning(false);
    keycloakMock.start();
    assertServerMockRunnning(true);
    keycloakMock.stop();
    assertServerMockRunnning(false);
  }

  @Test
  void mock_server_can_be_started_and_stopped_twice() {
    keycloakMock = new KeycloakMock();
    assertServerMockRunnning(false);
    keycloakMock.start();
    assertServerMockRunnning(true);
    keycloakMock.stop();
    assertServerMockRunnning(false);
    keycloakMock.start();
    assertServerMockRunnning(true);
    keycloakMock.stop();
    assertServerMockRunnning(false);
  }

  private void assertServerMockRunnning(boolean running) {
    try {
      when()
          .get("http://localhost:8000/auth/realms/master/protocol/openid-connect/certs")
          .then()
          .statusCode(200)
          .and()
          .contentType(ContentType.JSON);
      assertThat(running).as("Exception should occur if server is not running").isTrue();
    } catch (Throwable e) {
      assertThat(running).as("Exception should occur if server is not running").isFalse();
    }
  }

  @Test
  void mock_server_fails_when_port_is_claimed() {
    keycloakMock = new KeycloakMock();
    keycloakMock.start();
    KeycloakMock secondMock = new KeycloakMock();
    assertThatThrownBy(secondMock::start).isInstanceOf(MockServerException.class);
  }

  @ParameterizedTest
  @MethodSource("serverConfig")
  void mock_server_endpoint_is_correctly_configured(int port, boolean tls) {
    keycloakMock = new KeycloakMock(aServerConfig().withPort(port).withTls(tls).build());
    keycloakMock.start();
    given()
        .relaxedHTTPSValidation()
        .when()
        .get(
            (tls ? "https" : "http")
                + "://localhost:"
                + port
                + "/auth/realms/master/protocol/openid-connect/certs")
        .then()
        .statusCode(200)
        .and()
        .contentType(ContentType.JSON);
    keycloakMock.stop();
  }

  private static Stream<Arguments> serverConfig() {
    return Stream.of(Arguments.of(8000, false), Arguments.of(8001, true));
  }

  @Test
  void generated_token_is_valid() {
    keycloakMock = new KeycloakMock();
    keycloakMock.start();
    String accessToken = keycloakMock.getAccessToken(TokenConfig.aTokenConfig().build());

    List<JSONWebKey> jsonWebKeys =
        JSONWebKeySetHelper.retrieveKeysFromWellKnownConfiguration(
            "http://localhost:8000/auth/realms/master/.well-known/openid-configuration");
    Map<String, Verifier> verifierMap =
        jsonWebKeys.stream()
            .collect(
                Collectors.toMap(
                    k -> k.kid,
                    k -> {
                      PublicKey key = JSONWebKey.parse(k);
                      if (key instanceof RSAPublicKey) {
                        return RSAVerifier.newVerifier(key);
                      } else if (key instanceof ECPublicKey) {
                        return ECVerifier.newVerifier(key);
                      } else {
                        throw new IllegalArgumentException("Unexpected public key " + key);
                      }
                    }));

    JWT result = JWT.getDecoder().decode(accessToken, verifierMap);

    assertThat(result.isExpired()).isFalse();
  }

  @Test
  void well_known_configuration_works() {
    keycloakMock = new KeycloakMock();
    keycloakMock.start();
    ConfigurationResponse response =
        given()
            .header("Host", "server")
            .when()
            .get("http://localhost:8000/auth/realms/test/.well-known/openid-configuration")
            .then()
            .assertThat()
            .statusCode(200)
            .and()
            .contentType(ContentType.JSON)
            .and()
            .extract()
            .body()
            .as(ConfigurationResponse.class);

    assertThat(response.authorization_endpoint)
        .isEqualTo("http://server/auth/realms/test/protocol/openid-connect/auth");
    assertThat(response.end_session_endpoint)
        .isEqualTo("http://server/auth/realms/test/protocol/openid-connect/logout");
    assertThat(response.id_token_signing_alg_values_supported).containsExactly("RS256");
    assertThat(response.issuer).isEqualTo("http://server/auth/realms/test");
    assertThat(response.jwks_uri)
        .isEqualTo("http://server/auth/realms/test/protocol/openid-connect/certs");
    assertThat(response.response_types_supported)
        .containsExactlyInAnyOrder("code", "code id_token", "id_token", "token id_token");
    assertThat(response.subject_types_supported).containsExactly("public");
    assertThat(response.token_endpoint)
        .isEqualTo("http://server/auth/realms/test/protocol/openid-connect/token");
  }

  @Test
  void mock_server_uses_host_header_as_server_host() {
    String hostname = "server";
    keycloakMock = new KeycloakMock();
    keycloakMock.start();
    String issuer =
        given()
            .when()
            .header("Host", hostname)
            .get("http://localhost:8000/auth/realms/test/.well-known/openid-configuration")
            .then()
            .assertThat()
            .statusCode(200)
            .and()
            .extract()
            .jsonPath()
            .get("issuer");

    assertThat(issuer).isEqualTo("http://%s/auth/realms/test", hostname);
  }

  @Test
  void mock_server_answers_204_on_iframe_init() {
    keycloakMock = new KeycloakMock();
    keycloakMock.start();
    given()
        .when()
        .get(
            "http://localhost:8000/auth/realms/test/protocol/openid-connect/login-status-iframe.html/init")
        .then()
        .assertThat()
        .statusCode(204)
        .and()
        .body(is(emptyString()));
  }

  private static Stream<Arguments> resourcesWithContent() {
    return Stream.of(
        of(
            "/realms/test/protocol/openid-connect/login-status-iframe.html",
            HTML,
            "getSessionCookie()"),
        of("/realms/test/protocol/openid-connect/3p-cookies/step1.html", HTML, "step2.html"),
        of("/realms/test/protocol/openid-connect/3p-cookies/step2.html", HTML, "\"supported\""),
        of("/js/keycloak.js", JSON, "function Keycloak"),
        of("/js/vendor/web-crypto-shim/web-crypto-shim.js", JSON, "crypto.randomUUID"));
  }

  @ParameterizedTest
  @MethodSource("resourcesWithContent")
  void mock_server_properly_returns_resources(
      String resource, ContentType contentType, String content) {
    keycloakMock = new KeycloakMock();
    keycloakMock.start();
    String body =
        given()
            .when()
            .get("http://localhost:8000/auth" + resource)
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
  void iframe_has_correct_shim_path() {
    ServerConfig config = aServerConfig().build();
    keycloakMock = new KeycloakMock(config);
    UrlConfiguration configuration = new UrlConfiguration(config);

    keycloakMock.start();
    String body =
        given()
            .when()
            .get(
                "http://localhost:8000/auth/realms/test/protocol/openid-connect/login-status-iframe.html")
            .then()
            .assertThat()
            .statusCode(200)
            .and()
            .contentType(HTML)
            .extract()
            .body()
            .asString();

    assertThat(body).contains(IFrameRoute.getWebCryptoShimPath(configuration).toASCIIString());
  }

  @Test
  void mock_server_returns_404_on_nonexistent_resource() {
    keycloakMock = new KeycloakMock();
    keycloakMock.start();
    given().when().get("http://localhost:8000/i-do-not-exist").then().assertThat().statusCode(404);
  }

  @Test
  void mock_server_login_with_implicit_flow_works() throws Exception {
    keycloakMock = new KeycloakMock();
    keycloakMock.start();

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
    logoutAndExpectSessionCookieReset(Method.GET);
  }

  @Test
  void mock_server_logout_with_POST_works() throws Exception {
    keycloakMock = new KeycloakMock();
    keycloakMock.start();

    // open login page to create session (implicit flow)
    ClientRequest firstRequest = new ClientRequest("redirect-uri", "state", "nonce", "id_token");
    String callbackUrl = openLoginPageAndGetCallbackUrl(firstRequest);

    // simulate login
    loginAndValidateAndReturnSessionCookie(firstRequest, callbackUrl);

    // logout
    logoutAndExpectSessionCookieReset(Method.POST);
  }

  @Test
  void mock_server_login_with_authorization_code_flow_works() throws Exception {
    keycloakMock = new KeycloakMock();
    keycloakMock.start();

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
    logoutAndExpectSessionCookieReset(Method.GET);
  }

  private String openLoginPageAndGetCallbackUrl(ClientRequest request) {
    return given()
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
        given()
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
        given()
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
        given()
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
        given()
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
    Jws<Claims> jwt = jwtParser.parseSignedClaims(accessToken);
    assertThat(jwt.getPayload().getIssuer()).isEqualTo("http://localhost:8000/auth/realms/realm");
    TokenConfig tokenConfig = TokenConfig.aTokenConfig().withSourceToken(accessToken).build();
    assertThat(tokenConfig.getPreferredUsername()).isEqualTo("username");
    assertThat(tokenConfig.getRealmAccess().getRoles())
        .containsExactlyInAnyOrder("role1", "role2", "role3");
    assertThat(tokenConfig.getClaims()).containsEntry("nonce", nonce);
  }

  private void openLoginPageAgainAndExpectToBeLoggedInAlready(
      ClientRequest request, Cookie keycloakSession) throws URISyntaxException {
    String location =
        given()
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

  private void logoutAndExpectSessionCookieReset(Method method) {
    given()
        .config(config().redirect(redirectConfig().followRedirects(false)))
        .when()
        .request(
            method,
            "http://localhost:8000/auth/realms/realm/protocol/openid-connect/logout?post_logout_redirect_uri=redirect_uri")
        .then()
        .assertThat()
        .statusCode(302)
        .header("location", "redirect_uri")
        .cookie("KEYCLOAK_SESSION", "realm/dummy-user-id");
  }

  @Test
  void
      mock_server_login_with_resource_owner_password_credentials_flow_works_with_client_id_parameter() {
    keycloakMock = new KeycloakMock();
    keycloakMock.start();

    ExtractableResponse<Response> extractableResponse =
        given()
            .when()
            .formParam("client_id", "client")
            .formParam("username", "username")
            .formParam("password", "role1,role2,role3")
            .formParam("grant_type", "password")
            .post(TOKEN_ENDPOINT_URL)
            .then()
            .assertThat()
            .statusCode(200)
            .extract();

    String accessToken = extractableResponse.body().jsonPath().getString("access_token");

    Jws<Claims> jwt = jwtParser.parseSignedClaims(accessToken);
    assertThat(jwt.getPayload().getIssuer()).isEqualTo("http://localhost:8000/auth/realms/realm");
    TokenConfig tokenConfig = TokenConfig.aTokenConfig().withSourceToken(accessToken).build();
    assertThat(tokenConfig.getPreferredUsername()).isEqualTo("username");
    assertThat(tokenConfig.getRealmAccess().getRoles())
        .containsExactlyInAnyOrder("role1", "role2", "role3");
    assertThat(tokenConfig.getAudience()).containsExactlyInAnyOrder("client", "server");
  }

  @Test
  void mock_server_login_with_resource_owner_password_credentials_flow_works() {
    keycloakMock = new KeycloakMock();
    keycloakMock.start();

    ExtractableResponse<Response> extractableResponse =
        given()
            .auth()
            .preemptive()
            .basic("client", "does not matter")
            .when()
            .formParam("username", "username")
            .formParam("password", "role1,role2,role3")
            .formParam("grant_type", "password")
            .post(TOKEN_ENDPOINT_URL)
            .then()
            .assertThat()
            .statusCode(200)
            .extract();

    String accessToken = extractableResponse.body().jsonPath().getString("access_token");

    Jws<Claims> jwt = jwtParser.parseSignedClaims(accessToken);
    assertThat(jwt.getPayload().getIssuer()).isEqualTo("http://localhost:8000/auth/realms/realm");
    TokenConfig tokenConfig = TokenConfig.aTokenConfig().withSourceToken(accessToken).build();
    assertThat(tokenConfig.getPreferredUsername()).isEqualTo("username");
    assertThat(tokenConfig.getRealmAccess().getRoles())
        .containsExactlyInAnyOrder("role1", "role2", "role3");
    assertThat(tokenConfig.getAudience()).containsExactlyInAnyOrder("client", "server");
  }

  @Test
  void mock_server_login_with_client_credentials_flow_works() {
    keycloakMock = new KeycloakMock();
    keycloakMock.start();

    ExtractableResponse<Response> extractableResponse =
        given()
            .auth()
            .preemptive()
            .basic("client", "role1,role2,role3")
            .when()
            .formParam("grant_type", "client_credentials")
            .post(TOKEN_ENDPOINT_URL)
            .then()
            .assertThat()
            .statusCode(200)
            .extract();

    String accessToken = extractableResponse.body().jsonPath().getString("access_token");

    Jws<Claims> jwt = jwtParser.parseSignedClaims(accessToken);
    assertThat(jwt.getPayload().getIssuer()).isEqualTo("http://localhost:8000/auth/realms/realm");
    TokenConfig tokenConfig = TokenConfig.aTokenConfig().withSourceToken(accessToken).build();
    assertThat(tokenConfig.getPreferredUsername()).isEqualTo("client");
    assertThat(tokenConfig.getRealmAccess().getRoles())
        .containsExactlyInAnyOrder("role1", "role2", "role3");
    assertThat(tokenConfig.getAudience()).containsExactlyInAnyOrder("client", "server");
  }

  @Test
  void mock_server_login_with_client_credentials_flow_using_form_works() {
    keycloakMock = new KeycloakMock();
    keycloakMock.start();

    ExtractableResponse<Response> extractableResponse =
        given()
            .when()
            .formParam("grant_type", "client_credentials")
            .formParam("client_id", "client")
            .formParam("client_secret", "role1,role2,role3")
            .post(TOKEN_ENDPOINT_URL)
            .then()
            .assertThat()
            .statusCode(200)
            .extract();

    String accessToken = extractableResponse.body().jsonPath().getString("access_token");

    Jws<Claims> jwt = jwtParser.parseSignedClaims(accessToken);
    assertThat(jwt.getPayload().getIssuer()).isEqualTo("http://localhost:8000/auth/realms/realm");
    TokenConfig tokenConfig = TokenConfig.aTokenConfig().withSourceToken(accessToken).build();
    assertThat(tokenConfig.getPreferredUsername()).isEqualTo("client");
    assertThat(tokenConfig.getRealmAccess().getRoles())
        .containsExactlyInAnyOrder("role1", "role2", "role3");
    assertThat(tokenConfig.getAudience()).containsExactlyInAnyOrder("client", "server");
  }

  @Test
  void documentation_works() {
    keycloakMock = new KeycloakMock();
    keycloakMock.start();

    ExtractableResponse<Response> extractableResponse =
        given()
            .when()
            .get("http://localhost:8000/docs")
            .then()
            .assertThat()
            .statusCode(200)
            .extract();

    assertThat(extractableResponse.body().asPrettyString())
        .contains(
            "      <tr>\n"
                + "        <td colspan=\"1\" rowspan=\"1\">GET</td>\n"
                + "        <td colspan=\"1\" rowspan=\"1\">/docs</td>\n"
                + "        <td colspan=\"1\" rowspan=\"1\">documentation endpoint</td>\n"
                + "      </tr>");
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
}
