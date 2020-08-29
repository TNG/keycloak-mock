package com.tngtech.keycloakmock.standalone.handler;

import com.tngtech.keycloakmock.standalone.render.RenderHelper;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import javax.annotation.Nonnull;

public class KeycloakJsRoute implements Handler<RoutingContext> {
  @Nonnull private final RenderHelper renderHelper;

  public KeycloakJsRoute(@Nonnull final RenderHelper renderHelper) {
    this.renderHelper = renderHelper;
  }

  @Override
  public void handle(RoutingContext routingContext) {
    renderHelper.renderTemplate(routingContext, "keycloak.ftl", "application/javascript");
  }
}
