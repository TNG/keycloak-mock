package com.tngtech.keycloakmock.standalone.handler;

import com.tngtech.keycloakmock.standalone.render.RenderHelper;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class IframeRoute implements Handler<RoutingContext> {
  private final RenderHelper renderHelper;

  public IframeRoute(RenderHelper renderHelper) {
    this.renderHelper = renderHelper;
  }

  @Override
  public void handle(RoutingContext routingContext) {
    if (routingContext.normalizedPath().endsWith("/init")) {
      routingContext.response().setStatusCode(204).end();
    }
    renderHelper.renderTemplate(routingContext, "iframe.ftl", "text/html");
  }
}
