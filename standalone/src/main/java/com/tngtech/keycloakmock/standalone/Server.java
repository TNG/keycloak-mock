package com.tngtech.keycloakmock.standalone;

import static com.tngtech.keycloakmock.api.TokenConfig.aTokenConfig;

import com.tngtech.keycloakmock.api.KeycloakVerificationMock;
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

class Server extends KeycloakVerificationMock {
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
                  .putHeader(
                      "Access-Control-Allow-Origin",
                      routingContext.request().headers().get("Origin"))
                  .putHeader("Access-Control-Allow-Methods", "GET,POST")
                  .putHeader("Access-Control-Allow-Credentials", "true");
              routingContext.next();
            })
        .failureHandler(r -> r.failure().printStackTrace())
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
    router
        .get("/auth/realms/:realm/protocol/openid-connect/logout")
        .handler(this::logout);
    return router;
  }

  private void getLoginPage(final RoutingContext routingContext) {
    String sessionId = UUID.randomUUID().toString();
    routingContext.put("client_id", routingContext.queryParam("client_id").get(0));
    routingContext.put("state", routingContext.queryParam("state").get(0));
    routingContext.put("nonce", routingContext.queryParam("nonce").get(0));
    routingContext.put("redirect_uri", routingContext.queryParam("redirect_uri").get(0));
    String realm = routingContext.pathParam("realm");
    routingContext.put("realm", realm);
    routingContext.put("session_id", sessionId);
    renderTemplate(routingContext, "loginPage.ftl", "text/html");
  }

  private void authenticate(final RoutingContext routingContext) {
    String realm = routingContext.queryParam("realm").get(0);
    String sessionId = routingContext.queryParam("session_id").get(0);
    String token =
        getAccessTokenForRealm(
            aTokenConfig()
                .withAudience(routingContext.queryParam("client_id").get(0))
                .withIssuedAt(Instant.now())
                .withSubject(routingContext.queryParam("user").get(0))
                .withRealmRoles(
                    Arrays.asList(routingContext.queryParam("roles").get(0).trim().split(",")))
                .withClaim("nonce", routingContext.queryParam("nonce").get(0))
                .withClaim("session_state", sessionId)
                .build(),
            realm);
    tokens.put(sessionId, token);
    String redirectUri =
        routingContext.queryParam("redirect_uri").get(0)
            + "#state="
            + routingContext.queryParam("state").get(0)
            + "&session_state="
            + routingContext.queryParam("state").get(0)
            + "&code="
            + sessionId; // for simplicity, use session ID as authorization code
    setKeycloakSessionCookie(routingContext, realm, sessionId, 36000);
    routingContext.response().putHeader("location", redirectUri).setStatusCode(302).end();
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
    routingContext.put("session_state", code);
    renderTemplate(routingContext, "tokenResponse.ftl", "application/json");
  }

  private void getIframe(final RoutingContext routingContext) {
    renderTemplate(routingContext, "iframe.ftl", "text/html");
  }

  private void initIframe(final RoutingContext routingContext) {
    routingContext.response().setStatusCode(204).end();
  }

  private void logout(final RoutingContext routingContext) {
    String redirectUri = routingContext.queryParam("redirect_uri").get(0);
    String realm = routingContext.pathParam("realm");
    // invalidate session cookie
    setKeycloakSessionCookie(routingContext, realm, "", 0);
    routingContext.response().putHeader("location", redirectUri).setStatusCode(302).end();
  }

  private void setKeycloakSessionCookie(final RoutingContext routingContext, final String realm, final String sessionId, final long maxAge) {
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
            res.cause().printStackTrace();
            routingContext.fail(res.cause());
          }
        });
  }
}
