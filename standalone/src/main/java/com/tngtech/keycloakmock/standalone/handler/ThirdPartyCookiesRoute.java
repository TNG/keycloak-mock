package com.tngtech.keycloakmock.standalone.handler;

import com.tngtech.keycloakmock.standalone.render.RenderHelper;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class ThirdPartyCookiesRoute implements Handler<RoutingContext> {
  private final RenderHelper renderHelper;

  public ThirdPartyCookiesRoute(RenderHelper renderHelper) {
    this.renderHelper = renderHelper;
  }

  @Override
  public void handle(RoutingContext routingContext) {
    renderHelper.renderTemplate(routingContext, "3p-cookies.ftl", "text/html");
  }
}
