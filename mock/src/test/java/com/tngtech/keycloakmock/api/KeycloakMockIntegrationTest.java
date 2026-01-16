package com.tngtech.keycloakmock.api;

import static com.tngtech.keycloakmock.api.ServerConfig.aServerConfig;
import static com.tngtech.keycloakmock.api.TokenConfig.aTokenConfig;
import static com.tngtech.keycloakmock.test.KeyHelper.loadValidKey;
import static io.vertx.core.http.HttpResponseExpectation.JSON;
import static io.vertx.core.http.HttpResponseExpectation.SC_FOUND;
import static io.vertx.core.http.HttpResponseExpectation.SC_NOT_FOUND;
import static io.vertx.core.http.HttpResponseExpectation.SC_OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.MapEntry.entry;
import static org.junit.jupiter.params.provider.Arguments.of;

import com.tngtech.keycloakmock.impl.UrlConfiguration;
import com.tngtech.keycloakmock.impl.UrlConfigurationFactory;
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
import io.netty.handler.codec.http.cookie.ClientCookieDecoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpResponseExpectation;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.client.WebClientSession;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(VertxExtension.class)
class KeycloakMockIntegrationTest {

  private static final String LOGIN_PAGE_URL_TEMPLATE =
      "/auth/realms/realm/protocol/openid-connect/auth?client_id=client&redirect_uri=%s&state=%s&nonce=%s&response_type=%s";
  private static final String TOKEN_ENDPOINT_URL =
      "/auth/realms/realm/protocol/openid-connect/token";
  private static final HttpResponseExpectation HTML =
      HttpResponseExpectation.contentType("text/html");
  private static final HttpResponseExpectation JS =
      HttpResponseExpectation.contentType("text/javascript");

  private static JwtParser jwtParser;
  private KeycloakMock keycloakMock = null;

  @BeforeAll
  static void setupJwtsParser() {
    jwtParser = Jwts.parser().verifyWith(loadValidKey()).build();
  }

  @AfterEach
  void tearDown() {
    if (keycloakMock != null) {
      keycloakMock.stop();
    }
  }

  @Test
  void mock_server_can_be_started_and_stopped(Vertx vertx, VertxTestContext testContext) {
    WebClient webClient = WebClient.create(vertx);
    keycloakMock = new KeycloakMock();
    assertServerMockRunnning(webClient, testContext, false);
    keycloakMock.start();
    assertServerMockRunnning(webClient, testContext, true);
    keycloakMock.stop();
    assertServerMockRunnning(webClient, testContext, false);
    testContext.completeNow();
  }

  @Test
  void mock_server_can_be_started_and_stopped_twice(Vertx vertx, VertxTestContext testContext) {
    WebClient webClient = WebClient.create(vertx);
    keycloakMock = new KeycloakMock();
    assertServerMockRunnning(webClient, testContext, false);
    keycloakMock.start();
    assertServerMockRunnning(webClient, testContext, true);
    keycloakMock.stop();
    assertServerMockRunnning(webClient, testContext, false);
    keycloakMock.start();
    assertServerMockRunnning(webClient, testContext, true);
    keycloakMock.stop();
    assertServerMockRunnning(webClient, testContext, false);
    testContext.completeNow();
  }

  private void assertServerMockRunnning(
      WebClient webClient, VertxTestContext testContext, boolean running) {
    try {
      Future.await(
          webClient
              .get("/auth/realms/master/protocol/openid-connect/certs")
              .port(8000)
              .send()
              .expecting(SC_OK.and(JSON)),
          5,
          TimeUnit.SECONDS);
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
  void mock_server_endpoint_is_correctly_configured(
      int port, boolean tls, Vertx vertx, VertxTestContext testContext) {
    keycloakMock = new KeycloakMock(aServerConfig().withPort(port).withTls(tls).build());
    keycloakMock.start();
    if (port > 0) {
      assertThat(keycloakMock.getActualPort()).isEqualTo(port);
    } else {
      assertThat(keycloakMock.getActualPort()).isGreaterThan(0);
    }
    WebClient.create(vertx, new WebClientOptions().setSsl(tls).setTrustAll(true))
        .get("/auth/realms/master/protocol/openid-connect/certs")
        .port(keycloakMock.getActualPort())
        .ssl(tls)
        .send()
        .expecting(SC_OK.and(JSON))
        .onComplete(
            testContext.succeeding(
                r -> {
                  testContext.completeNow();
                  keycloakMock.stop();
                }));
  }

  private static Stream<Arguments> serverConfig() {
    return Stream.of(Arguments.of(8000, false), Arguments.of(8001, true), Arguments.of(0, true));
  }

  @Test
  void generated_token_is_valid() {
    keycloakMock = new KeycloakMock();
    keycloakMock.start();
    String accessToken = keycloakMock.getAccessToken(aTokenConfig().build());

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
  void well_known_configuration_works(Vertx vertx, VertxTestContext testContext) {
    keycloakMock = new KeycloakMock();
    keycloakMock.start();
    WebClient.create(vertx)
        .get("/auth/realms/test/.well-known/openid-configuration")
        .port(8000)
        .putHeader("Host", "server")
        .send()
        .expecting(SC_OK.and(JSON))
        .map(r -> r.bodyAsJson(ConfigurationResponse.class))
        .expecting(
            config -> {
              assertThat(config.authorization_endpoint)
                  .isEqualTo("http://server/auth/realms/test/protocol/openid-connect/auth");
              assertThat(config.end_session_endpoint)
                  .isEqualTo("http://server/auth/realms/test/protocol/openid-connect/logout");
              assertThat(config.id_token_signing_alg_values_supported).containsExactly("RS256");
              assertThat(config.issuer).isEqualTo("http://server/auth/realms/test");
              assertThat(config.jwks_uri)
                  .isEqualTo("http://server/auth/realms/test/protocol/openid-connect/certs");
              assertThat(config.response_types_supported)
                  .containsExactlyInAnyOrder("code", "code id_token", "id_token", "token id_token");
              assertThat(config.subject_types_supported).containsExactly("public");
              assertThat(config.token_endpoint)
                  .isEqualTo("http://server/auth/realms/test/protocol/openid-connect/token");
              assertThat(config.introspection_endpoint)
                  .isEqualTo(
                      "http://server/auth/realms/test/protocol/openid-connect/token/introspect");
              return true;
            })
        .onComplete(testContext.succeedingThenComplete());
  }

  @Test
  void mock_server_answers_204_on_iframe_init(Vertx vertx, VertxTestContext testContext) {
    keycloakMock = new KeycloakMock();
    keycloakMock.start();
    WebClient.create(vertx)
        .get("/auth/realms/test/protocol/openid-connect/login-status-iframe.html/init")
        .port(8000)
        .send()
        .expecting(HttpResponseExpectation.SC_NO_CONTENT)
        .map(HttpResponse::bodyAsString)
        .expecting(Objects::isNull)
        .onComplete(testContext.succeedingThenComplete());
  }

  private static Stream<Arguments> resourcesWithContent() {
    return Stream.of(
        of(
            "/auth/realms/test/protocol/openid-connect/login-status-iframe.html",
            HTML,
            "getSessionCookie()"),
        of("/auth/realms/test/protocol/openid-connect/3p-cookies/step1.html", HTML, "step2.html"),
        of(
            "/auth/realms/test/protocol/openid-connect/3p-cookies/step2.html",
            HTML,
            "\"supported\""),
        of("/auth/js/keycloak.js", JS, "function Keycloak"),
        of("/auth/js/vendor/web-crypto-shim/web-crypto-shim.js", JS, "crypto.randomUUID"));
  }

  @ParameterizedTest
  @MethodSource("resourcesWithContent")
  void mock_server_properly_returns_resources(
      String resource,
      HttpResponseExpectation contentType,
      String content,
      Vertx vertx,
      VertxTestContext testContext) {
    keycloakMock = new KeycloakMock();
    keycloakMock.start();
    WebClient.create(vertx)
        .get(resource)
        .port(8000)
        .send()
        .expecting(SC_OK.and(contentType))
        .map(HttpResponse::bodyAsString)
        .expecting(
            body -> {
              assertThat(body).contains(content);
              return true;
            })
        .onComplete(testContext.succeedingThenComplete());
  }

  @Test
  void iframe_has_correct_shim_path(Vertx vertx, VertxTestContext testContext) {
    ServerConfig config = aServerConfig().build();
    keycloakMock = new KeycloakMock(config);
    UrlConfiguration configuration = new UrlConfigurationFactory(config).create(null, null);

    keycloakMock.start();
    WebClient.create(vertx)
        .get("/auth/realms/test/protocol/openid-connect/login-status-iframe.html")
        .port(8000)
        .send()
        .expecting(SC_OK.and(HTML))
        .map(HttpResponse::bodyAsString)
        .expecting(
            body -> {
              assertThat(body)
                  .contains(IFrameRoute.getWebCryptoShimPath(configuration).toASCIIString());
              return true;
            })
        .onComplete(testContext.succeedingThenComplete());
  }

  @Test
  void mock_server_returns_404_on_nonexistent_resource(Vertx vertx, VertxTestContext testContext) {
    keycloakMock = new KeycloakMock();
    keycloakMock.start();
    WebClient.create(vertx)
        .get("/i-do-not-exist")
        .port(8000)
        .send()
        .expecting(SC_NOT_FOUND)
        .onComplete(testContext.succeedingThenComplete());
  }

  @Test
  void mock_server_login_with_implicit_flow_works(Vertx vertx, VertxTestContext testContext)
      throws Exception {
    keycloakMock = new KeycloakMock();
    keycloakMock.start();

    // use a session to retain cookies
    WebClientSession webClientSession = WebClientSession.create(WebClient.create(vertx));

    // open login page to create session (implicit flow)
    ClientRequest firstRequest = new ClientRequest("redirect-uri", "state", "nonce", "id_token");
    String callbackUrl = openLoginPageAndGetCallbackUrl(firstRequest, webClientSession);

    // simulate login
    String sessionId =
        loginAndValidateAndReturnSessionCookie(firstRequest, callbackUrl, webClientSession);

    // subsequent request to login page with session cookie will immediately return
    ClientRequest secondRequest =
        new ClientRequest("redirect-uri2", "state2", "nonce2", "id_token");
    openLoginPageAgainAndExpectToBeLoggedInAlready(secondRequest, sessionId, webClientSession);

    // logout
    logoutAndExpectSessionCookieReset(HttpMethod.GET, webClientSession, testContext);
    testContext.completeNow();
  }

  @Test
  void mock_server_logout_with_POST_works(Vertx vertx, VertxTestContext testContext)
      throws Exception {
    keycloakMock = new KeycloakMock();
    keycloakMock.start();

    WebClient webClient = WebClient.create(vertx);

    // open login page to create session (implicit flow)
    ClientRequest firstRequest = new ClientRequest("redirect-uri", "state", "nonce", "id_token");
    String callbackUrl = openLoginPageAndGetCallbackUrl(firstRequest, webClient);

    // simulate login
    loginAndValidateAndReturnSessionCookie(firstRequest, callbackUrl, webClient);

    // logout
    logoutAndExpectSessionCookieReset(HttpMethod.POST, webClient, testContext);
    testContext.completeNow();
  }

  @Test
  void mock_server_login_with_authorization_code_flow_works(
      Vertx vertx, VertxTestContext testContext) throws Exception {
    keycloakMock = new KeycloakMock();
    keycloakMock.start();

    WebClient webClient = WebClient.create(vertx);

    // open login page to create session (authorization code flow)
    ClientRequest firstRequest = new ClientRequest("redirect-uri", "state", "nonce", "code");
    String callbackUrl = openLoginPageAndGetCallbackUrl(firstRequest, webClient);

    // simulate login
    String authorizationCode =
        loginAndValidateAndReturnAuthCode(firstRequest, callbackUrl, webClient);
    String refreshToken =
        validateAuthorizationAndRetrieveToken(
            authorizationCode, firstRequest.getNonce(), webClient);

    // refresh token flow
    validateRefreshTokenFlow(refreshToken, firstRequest.getNonce(), webClient);

    // logout
    logoutAndExpectSessionCookieReset(HttpMethod.GET, webClient, testContext);
  }

  private String openLoginPageAndGetCallbackUrl(ClientRequest request, WebClient webClient) {
    String body =
        Future.await(
            webClient
                .get(request.getLoginPageUrl())
                .port(8000)
                .send()
                .expecting(SC_OK.and(HTML))
                .map(HttpResponse::bodyAsString));
    return Optional.ofNullable(Jsoup.parse(body).body().selectFirst("form"))
        .map(e -> e.attr("action"))
        .orElse("");
  }

  private String loginAndValidateAndReturnSessionCookie(
      ClientRequest request, String callbackUrl, WebClient webClient) throws URISyntaxException {
    HttpResponse<Buffer> response =
        Future.await(
            webClient
                .post(callbackUrl)
                .port(8000)
                .followRedirects(false)
                .sendForm(
                    MultiMap.caseInsensitiveMultiMap()
                        .add("username", "username")
                        .add("password", "role1,role2,role3"))
                .expecting(SC_FOUND));
    String location = response.getHeader("location");
    Cookie keycloakSession =
        response.cookies().stream()
            .map(ClientCookieDecoder.STRICT::decode)
            .filter(c -> c.name().equals("KEYCLOAK_SESSION"))
            .findFirst()
            .orElse(new DefaultCookie("KEYCLOAK_SESSION", ""));
    String sessionId = validateCookieAndReturnSessionId(keycloakSession);

    validateProperlyLoggedInAndRedirected(request, sessionId, location);
    return sessionId;
  }

  private String loginAndValidateAndReturnAuthCode(
      ClientRequest request, String callbackUrl, WebClient webClient) throws URISyntaxException {
    HttpResponse<Buffer> response =
        Future.await(
            webClient
                .post(callbackUrl)
                .port(8000)
                .followRedirects(false)
                .sendForm(
                    MultiMap.caseInsensitiveMultiMap()
                        .add("username", "username")
                        .add("password", "role1,role2,role3"))
                .expecting(SC_FOUND));
    String location = response.getHeader("location");
    Cookie keycloakSession =
        response.cookies().stream()
            .map(ClientCookieDecoder.STRICT::decode)
            .filter(c -> c.name().equals("KEYCLOAK_SESSION"))
            .findFirst()
            .orElse(new DefaultCookie("KEYCLOAK_SESSION", ""));

    return validateProperlyRedirectedWithAuthorizationCode(request, keycloakSession, location);
  }

  private String validateAuthorizationAndRetrieveToken(
      String authorizationCode, String nonce, WebClient webClient) {
    JsonObject json =
        Future.await(
            webClient
                .post(TOKEN_ENDPOINT_URL)
                .port(8000)
                .followRedirects(false)
                .sendForm(
                    MultiMap.caseInsensitiveMultiMap()
                        .add("grant_type", "authorization_code")
                        .add("code", authorizationCode))
                .expecting(SC_OK)
                .map(HttpResponse::bodyAsJsonObject));
    String accessToken = json.getString("access_token");

    validateToken(accessToken, nonce);

    return json.getString("refresh_token");
  }

  private void validateRefreshTokenFlow(String refreshToken, String nonce, WebClient webClient) {
    JsonObject json =
        Future.await(
            webClient
                .post(TOKEN_ENDPOINT_URL)
                .port(8000)
                .followRedirects(false)
                .sendForm(
                    MultiMap.caseInsensitiveMultiMap()
                        .add("grant_type", "refresh_token")
                        .add("refresh_token", refreshToken))
                .expecting(SC_OK)
                .map(HttpResponse::bodyAsJsonObject));
    String accessToken = json.getString("access_token");

    validateToken(accessToken, nonce);
  }

  private void validateToken(String accessToken, String nonce) {
    Jws<Claims> jwt = jwtParser.parseSignedClaims(accessToken);
    assertThat(jwt.getPayload().getIssuer()).isEqualTo("http://localhost:8000/auth/realms/realm");
    TokenConfig tokenConfig = aTokenConfig().withSourceToken(accessToken).build();
    assertThat(tokenConfig.getPreferredUsername()).isEqualTo("username");
    assertThat(tokenConfig.getRealmAccess().getRoles())
        .containsExactlyInAnyOrder("role1", "role2", "role3");
    assertThat(tokenConfig.getClaims()).containsEntry("nonce", nonce);
  }

  private void openLoginPageAgainAndExpectToBeLoggedInAlready(
      ClientRequest request, String sessionId, WebClient webClient) throws URISyntaxException {
    HttpResponse<Buffer> response =
        Future.await(
            webClient
                .get(request.getLoginPageUrl())
                .port(8000)
                .followRedirects(false)
                .send()
                .expecting(SC_FOUND));
    String location = response.getHeader("location");

    validateProperlyLoggedInAndRedirected(request, sessionId, location);
  }

  private void validateProperlyLoggedInAndRedirected(
      ClientRequest request, String sessionId, String location) throws URISyntaxException {
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
    assertThat(keycloakSession.path()).isEqualTo("/auth/realms/realm/");
    assertThat(keycloakSession.maxAge()).isEqualTo(36000);
    String[] components = keycloakSession.value().split("/");
    assertThat(components).hasSize(3);
    assertThat(components[0]).isEqualTo("realm");
    return components[2];
  }

  private void logoutAndExpectSessionCookieReset(
      HttpMethod method, WebClient webClient, VertxTestContext testContext) {
    webClient
        .request(
            method,
            "/auth/realms/realm/protocol/openid-connect/logout?post_logout_redirect_uri=redirect_uri")
        .port(8000)
        .followRedirects(false)
        .send()
        .expecting(SC_FOUND)
        .expecting(
            response -> {
              assertThat(response.getHeader("Location")).isEqualTo("redirect_uri");
              assertThat(response.cookies())
                  .hasSize(1)
                  .first()
                  .asString()
                  .startsWith("KEYCLOAK_SESSION=realm/dummy-user-id;");
              return true;
            })
        .onComplete(testContext.succeedingThenComplete());
  }

  @Test
  void
      mock_server_login_with_resource_owner_password_credentials_flow_works_with_client_id_parameter(
          Vertx vertx, VertxTestContext testContext) {
    keycloakMock = new KeycloakMock();
    keycloakMock.start();

    WebClient.create(vertx)
        .post(TOKEN_ENDPOINT_URL)
        .port(8000)
        .sendForm(
            MultiMap.caseInsensitiveMultiMap()
                .add("client_id", "client")
                .add("username", "username")
                .add("password", "role1,role2,role3")
                .add("grant_type", "password"))
        .expecting(SC_OK)
        .map(HttpResponse::bodyAsJsonObject)
        .expecting(
            json -> {
              String accessToken = json.getString("access_token");

              Jws<Claims> jwt = jwtParser.parseSignedClaims(accessToken);
              assertThat(jwt.getPayload().getIssuer())
                  .isEqualTo("http://localhost:8000/auth/realms/realm");
              TokenConfig tokenConfig = aTokenConfig().withSourceToken(accessToken).build();
              assertThat(tokenConfig.getPreferredUsername()).isEqualTo("username");
              assertThat(tokenConfig.getRealmAccess().getRoles())
                  .containsExactlyInAnyOrder("role1", "role2", "role3");
              assertThat(tokenConfig.getAudience()).containsExactlyInAnyOrder("client", "server");
              return true;
            })
        .onComplete(testContext.succeedingThenComplete());
  }

  @Test
  void mock_server_login_with_resource_owner_password_credentials_flow_works(
      Vertx vertx, VertxTestContext testContext) {
    keycloakMock = new KeycloakMock();
    keycloakMock.start();

    WebClient.create(vertx)
        .post(TOKEN_ENDPOINT_URL)
        .port(8000)
        .basicAuthentication("client", "does not matter")
        .sendForm(
            MultiMap.caseInsensitiveMultiMap()
                .add("username", "username")
                .add("password", "role1,role2,role3")
                .add("grant_type", "password"))
        .expecting(SC_OK)
        .map(HttpResponse::bodyAsJsonObject)
        .expecting(
            json -> {
              String accessToken = json.getString("access_token");

              Jws<Claims> jwt = jwtParser.parseSignedClaims(accessToken);
              assertThat(jwt.getPayload().getIssuer())
                  .isEqualTo("http://localhost:8000/auth/realms/realm");
              TokenConfig tokenConfig = aTokenConfig().withSourceToken(accessToken).build();
              assertThat(tokenConfig.getPreferredUsername()).isEqualTo("username");
              assertThat(tokenConfig.getRealmAccess().getRoles())
                  .containsExactlyInAnyOrder("role1", "role2", "role3");
              assertThat(tokenConfig.getAudience()).containsExactlyInAnyOrder("client", "server");
              return true;
            })
        .onComplete(testContext.succeedingThenComplete());
  }

  @Test
  void mock_server_login_with_client_credentials_flow_works(
      Vertx vertx, VertxTestContext testContext) {
    keycloakMock = new KeycloakMock();
    keycloakMock.start();

    WebClient.create(vertx)
        .post(TOKEN_ENDPOINT_URL)
        .port(8000)
        .basicAuthentication("client", "role1,role2,role3")
        .sendForm(MultiMap.caseInsensitiveMultiMap().add("grant_type", "client_credentials"))
        .expecting(SC_OK)
        .map(HttpResponse::bodyAsJsonObject)
        .expecting(
            json -> {
              String accessToken = json.getString("access_token");

              Jws<Claims> jwt = jwtParser.parseSignedClaims(accessToken);
              assertThat(jwt.getPayload().getIssuer())
                  .isEqualTo("http://localhost:8000/auth/realms/realm");
              TokenConfig tokenConfig = aTokenConfig().withSourceToken(accessToken).build();
              assertThat(tokenConfig.getPreferredUsername()).isEqualTo("client");
              assertThat(tokenConfig.getRealmAccess().getRoles())
                  .containsExactlyInAnyOrder("role1", "role2", "role3");
              assertThat(tokenConfig.getAudience()).containsExactlyInAnyOrder("client", "server");
              return true;
            })
        .onComplete(testContext.succeedingThenComplete());
  }

  @Test
  void mock_server_login_with_client_credentials_flow_using_form_works(
      Vertx vertx, VertxTestContext testContext) {
    keycloakMock = new KeycloakMock();
    keycloakMock.start();

    WebClient.create(vertx)
        .post(TOKEN_ENDPOINT_URL)
        .port(8000)
        .sendForm(
            MultiMap.caseInsensitiveMultiMap()
                .add("client_id", "client")
                .add("client_secret", "role1,role2,role3")
                .add("grant_type", "client_credentials"))
        .expecting(SC_OK)
        .map(HttpResponse::bodyAsJsonObject)
        .expecting(
            json -> {
              String accessToken = json.getString("access_token");

              Jws<Claims> jwt = jwtParser.parseSignedClaims(accessToken);
              assertThat(jwt.getPayload().getIssuer())
                  .isEqualTo("http://localhost:8000/auth/realms/realm");
              TokenConfig tokenConfig = aTokenConfig().withSourceToken(accessToken).build();
              assertThat(tokenConfig.getPreferredUsername()).isEqualTo("client");
              assertThat(tokenConfig.getRealmAccess().getRoles())
                  .containsExactlyInAnyOrder("role1", "role2", "role3");
              assertThat(tokenConfig.getAudience()).containsExactlyInAnyOrder("client", "server");
              return true;
            })
        .onComplete(testContext.succeedingThenComplete());
  }

  @Test
  void documentation_works(Vertx vertx, VertxTestContext testContext) {
    keycloakMock = new KeycloakMock();
    keycloakMock.start();

    WebClient.create(vertx)
        .get("/docs")
        .port(8000)
        .send()
        .expecting(SC_OK)
        .map(HttpResponse::bodyAsString)
        .expecting(
            body -> {
              assertThat(body)
                  .contains(
                      "    <tr>\n"
                          + "      <td>GET</td>\n"
                          + "      <td>/docs</td>\n"
                          + "      <td>documentation endpoint</td>\n"
                          + "    </tr>");
              return true;
            })
        .onComplete(testContext.succeedingThenComplete());
  }

  @Test
  void token_introspection_works(Vertx vertx, VertxTestContext testContext) {
    keycloakMock = new KeycloakMock();
    keycloakMock.start();

    TokenConfig tokenConfig = aTokenConfig().withAudience("myclient").build();
    String accessToken = keycloakMock.getAccessToken(tokenConfig);

    WebClient.create(vertx)
        .post("/auth/realms/realm/protocol/openid-connect/token/introspect")
        .port(8000)
        .sendForm(
            MultiMap.caseInsensitiveMultiMap()
                .add("token", accessToken)
                .add("client_id", "myclient"))
        .expecting(SC_OK)
        .map(HttpResponse::bodyAsJsonObject)
        .expecting(
            json -> {
              assertThat(json.getBoolean("active")).isTrue();
              assertThat(json.getMap()).containsAllEntriesOf(tokenConfig.getClaims());
              return true;
            })
        .onComplete(testContext.succeedingThenComplete());
  }

  @Test
  void token_introspection_does_not_leak_claims_on_invalid_token(
      Vertx vertx, VertxTestContext testContext) {
    keycloakMock = new KeycloakMock();
    keycloakMock.start();

    String accessToken =
        keycloakMock.getAccessToken(
            aTokenConfig().withExpiration(Instant.now().minus(1, ChronoUnit.HOURS)).build());

    WebClient.create(vertx)
        .post("/auth/realms/realm/protocol/openid-connect/token/introspect")
        .port(8000)
        .sendForm(
            MultiMap.caseInsensitiveMultiMap()
                .add("token", accessToken)
                .add("client_id", "myclient"))
        .expecting(SC_OK)
        .map(HttpResponse::bodyAsJsonObject)
        .expecting(
            json -> {
              assertThat(json.getMap()).containsExactly(entry("active", false));
              return true;
            })
        .onComplete(testContext.succeedingThenComplete());
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
