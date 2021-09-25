package com.tngtech.keycloakmock.impl.handler;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class FailureHandler implements Handler<RoutingContext> {
  private static final Logger LOG = LoggerFactory.getLogger(FailureHandler.class);

  @Inject
  FailureHandler() {}

  @Override
  public void handle(@Nonnull RoutingContext routingContext) {
    LOG.error("Error while accessing route", routingContext.failure());
    routingContext.next();
  }
}
