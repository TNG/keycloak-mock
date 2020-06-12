package com.tngtech.keycloakmock.standalone;

import static com.tngtech.keycloakmock.api.TokenConfig.aTokenConfig;

import com.tngtech.keycloakmock.api.KeycloakMock;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.common.template.TemplateEngine;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.ErrorHandler;
import io.vertx.ext.web.templ.freemarker.FreeMarkerTemplateEngine;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Server extends KeycloakMock {
  private static final Logger LOG = LoggerFactory.getLogger(Server.class);

  private static final String CLIENT_ID = "client_id";
  private static final String STATE = "state";
  private static final String NONCE = "nonce";
  private static final String REDIRECT_URI = "redirect_uri";
  private static final String REALM = "realm";
  private static final String SESSION_ID = "session_id";
  private static final String RESPONSE_TYPE = "response_type";
  private static final String RESPONSE_MODE = "response_mode";
  private static final String SESSION_STATE = "session_state";
  private final Map<String, String> tokens = new HashMap<>();
  private final TemplateEngine engine = FreeMarkerTemplateEngine.create(vertx);

  Server(final int port, final boolean tls) {
    super(port, "master", tls);
    start();
  }

  @Override
  protected Router configureRouter() {
    Router router = super.configureRouter();
    router
        .route()
        .handler(
            routingContext -> {
              routingContext
                  .response()
                  .bodyEndHandler(
                      aVoid ->
                          LOG.info(
                              "{}: {} {}",
                              routingContext.response().getStatusCode(),
                              routingContext.request().rawMethod(),
                              routingContext.request().uri()))
                  .putHeader(
                      "Access-Control-Allow-Origin",
                      routingContext.request().headers().get("Origin"))
                  .putHeader("Access-Control-Allow-Methods", "GET,POST")
                  .putHeader("Access-Control-Allow-Credentials", "true");
              routingContext.next();
            })
        .failureHandler(r -> LOG.error("Error while accessing route", r.failure()))
        .failureHandler(ErrorHandler.create());
    router.get("/auth/realms/:realm/protocol/openid-connect/auth").handler(this::getLoginPage);
    router.get("/authenticate").handler(this::authenticate);
    router
        .post("/auth/realms/:realm/protocol/openid-connect/token")
        .handler(BodyHandler.create())
        .handler(this::requestToken);
    router
        .get("/auth/realms/:realm/protocol/openid-connect/login-status-iframe.html")
        .handler(this::getIframe);
    router
        .get("/auth/realms/:realm/protocol/openid-connect/login-status-iframe.html/init")
        .handler(this::initIframe);
    router.get("/auth/realms/:realm/protocol/openid-connect/logout").handler(this::logout);
    return router;
  }

  private void getLoginPage(final RoutingContext routingContext) {
    String sessionId = UUID.randomUUID().toString();
    routingContext.put(CLIENT_ID, routingContext.queryParams().get(CLIENT_ID));
    routingContext.put(STATE, routingContext.queryParams().get(STATE));
    routingContext.put(NONCE, routingContext.queryParams().get(NONCE));
    routingContext.put(REDIRECT_URI, routingContext.queryParams().get(REDIRECT_URI));
    String realm = routingContext.pathParam(REALM);
    routingContext.put(REALM, realm);
    routingContext.put(SESSION_ID, sessionId);
    routingContext.put(RESPONSE_TYPE, routingContext.queryParams().get(RESPONSE_TYPE));
    // optional parameter
    routingContext.put(RESPONSE_MODE, routingContext.queryParams().get(RESPONSE_MODE));
    renderTemplate(routingContext, "loginPage.ftl", "text/html");
  }

  private void authenticate(final RoutingContext routingContext) {
    ResponseType responseType =
        ResponseType.fromValueOrNull(routingContext.queryParams().get(RESPONSE_TYPE));
    if (responseType == null) {
      LOG.warn(
          "Invalid response type '{}' requested!", routingContext.queryParams().get(RESPONSE_TYPE));
      routingContext.fail(400);
      return;
    }
    String realm = routingContext.queryParams().get(REALM);
    String sessionId = routingContext.queryParams().get(SESSION_ID);
    // for simplicity, the access token is the same as the ID token
    String token =
        getAccessTokenForRealm(
            aTokenConfig()
                .withAudience(routingContext.queryParams().get(CLIENT_ID))
                .withIssuedAt(Instant.now())
                .withSubject(routingContext.queryParams().get("user"))
                .withRealmRoles(
                    Arrays.asList(routingContext.queryParams().get("roles").trim().split(",")))
                .withClaim(NONCE, routingContext.queryParams().get(NONCE))
                .withClaim(SESSION_STATE, sessionId)
                .build(),
            realm);
    tokens.put(sessionId, token);
    ResponseMode responseMode =
        responseType.getValidResponseMode(routingContext.queryParams().get(RESPONSE_MODE));
    StringBuilder redirectUri = new StringBuilder(routingContext.queryParams().get(REDIRECT_URI));
    redirectUri.append(
        getResponseParameter(responseMode, STATE, routingContext.queryParams().get(STATE)));
    redirectUri.append(
        getResponseParameter(null, SESSION_STATE, routingContext.queryParams().get(STATE)));
    switch (responseType) {
      case CODE:
        // for simplicity, use session ID as authorization code
        redirectUri.append(getResponseParameter(null, "code", sessionId));
        break;
      case ID_TOKEN:
        redirectUri.append(getResponseParameter(null, "id_token", token));
        break;
      case ID_TOKEN_PLUS_TOKEN:
        redirectUri.append(getResponseParameter(null, "id_token", token));
        redirectUri.append(getResponseParameter(null, "access_token", token));
        redirectUri.append(getResponseParameter(null, "token_type", "bearer"));
        break;
      case NONE:
      default:
        break;
    }
    setKeycloakSessionCookie(routingContext, realm, sessionId, 36000);
    routingContext
        .response()
        .putHeader("location", redirectUri.toString())
        .setStatusCode(302)
        .end();
  }

  private String getResponseParameter(
      @Nullable final ResponseMode responseMode, final String name, final String value) {
    return (responseMode != null ? responseMode.getSign() : "&") + name + "=" + value;
  }

  private void requestToken(final RoutingContext routingContext) {
    if (!"authorization_code".equals(routingContext.request().getFormAttribute("grant_type"))) {
      routingContext.fail(400);
      return;
    }
    // here again we use the equality of authorization code and session ID
    String code = routingContext.request().getFormAttribute("code");
    String token = tokens.get(code);
    if (token == null) {
      routingContext.fail(404);
      return;
    }
    routingContext.put("token", token);
    routingContext.put(SESSION_STATE, code);
    renderTemplate(routingContext, "tokenResponse.ftl", "application/json");
  }

  private void getIframe(final RoutingContext routingContext) {
    renderTemplate(routingContext, "iframe.ftl", "text/html");
  }

  private void initIframe(final RoutingContext routingContext) {
    routingContext.response().setStatusCode(204).end();
  }

  private void logout(final RoutingContext routingContext) {
    String redirectUri = routingContext.queryParams().get(REDIRECT_URI);
    String realm = routingContext.pathParam(REALM);
    // invalidate session cookie
    setKeycloakSessionCookie(routingContext, realm, "", 0);
    routingContext.response().putHeader("location", redirectUri).setStatusCode(302).end();
  }

  private void setKeycloakSessionCookie(
      final RoutingContext routingContext,
      final String realm,
      final String sessionId,
      final long maxAge) {
    routingContext.addCookie(
        Cookie.cookie("KEYCLOAK_SESSION", realm + "/no-idea-what-goes-here/" + sessionId)
            .setPath("/auth/realms/" + realm + "/")
            .setMaxAge(maxAge)
            .setSecure(false));
  }

  private void renderTemplate(
      final RoutingContext routingContext, final String name, final String contentType) {
    engine.render(
        new JsonObject(routingContext.data()),
        name,
        res -> {
          if (res.succeeded()) {
            routingContext
                .response()
                .putHeader(HttpHeaders.CONTENT_TYPE, contentType)
                .end(res.result());
          } else {
            LOG.error("Unable to render template {}", name, res.cause());
            routingContext.fail(res.cause());
          }
        });
  }
}
