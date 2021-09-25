package com.tngtech.keycloakmock.impl.handler;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class IFrameRoute implements Handler<RoutingContext> {

  @Nonnull private final ResourceFileHandler resourceFileHandler;

  @Inject
  IFrameRoute(@Nonnull @Named("iframe") ResourceFileHandler resourceFileHandler) {
    this.resourceFileHandler = resourceFileHandler;
  }

  @Override
  public void handle(@Nonnull RoutingContext routingContext) {
    if (routingContext.normalizedPath().endsWith("/init")) {
      routingContext.response().setStatusCode(204).end();
      return;
    }
    resourceFileHandler.handle(routingContext);
  }
}
