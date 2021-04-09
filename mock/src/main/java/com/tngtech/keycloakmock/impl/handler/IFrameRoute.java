package com.tngtech.keycloakmock.impl.handler;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import javax.annotation.Nonnull;

public class IFrameRoute implements Handler<RoutingContext> {

  @Nonnull private final ResourceFileHandler resourceFileHandler;

  public IFrameRoute() {
    this.resourceFileHandler = new ResourceFileHandler("/login-status-iframe.html");
  }

  @Override
  public void handle(RoutingContext routingContext) {
    if (routingContext.normalizedPath().endsWith("/init")) {
      routingContext.response().setStatusCode(204).end();
      return;
    }
    resourceFileHandler.handle(routingContext);
  }
}
