package com.tngtech.keycloakmock.standalone.handler;

import com.tngtech.keycloakmock.standalone.render.RenderHelper;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import javax.annotation.Nonnull;

public class ThirdPartyCookiesRoute implements Handler<RoutingContext> {
  @Nonnull private final RenderHelper renderHelper;

  public ThirdPartyCookiesRoute(@Nonnull final RenderHelper renderHelper) {
    this.renderHelper = renderHelper;
  }

  @Override
  public void handle(@Nonnull final RoutingContext routingContext) {
    renderHelper.renderTemplate(routingContext, "3p-cookies.ftl", "text/html");
  }
}
