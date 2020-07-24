package com.tngtech.keycloakmock.standalone.handler;

import io.vertx.core.Handler;
import io.vertx.core.http.Cookie;
import io.vertx.ext.web.RoutingContext;

public class LogoutRoute implements Handler<RoutingContext> {
  private static final String REDIRECT_URI = "redirect_uri";
  private static final String REALM = "realm";

  @Override
  public void handle(RoutingContext routingContext) {
    String redirectUri = routingContext.queryParams().get(REDIRECT_URI);
    String realm = routingContext.pathParam(REALM);
    routingContext
        // invalidate session cookie
        .addCookie(
            Cookie.cookie("KEYCLOAK_SESSION", realm + "/no-idea-what-goes-here/")
                .setPath("/auth/realms/" + realm + "/")
                .setMaxAge(0)
                .setSecure(false))
        .response()
        .putHeader("location", redirectUri)
        .setStatusCode(302)
        .end();
  }
}
