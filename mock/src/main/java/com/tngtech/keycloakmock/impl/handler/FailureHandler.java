package com.tngtech.keycloakmock.impl.handler;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FailureHandler implements Handler<RoutingContext> {
  private static final Logger LOG = LoggerFactory.getLogger(FailureHandler.class);

  @Override
  public void handle(@Nonnull final RoutingContext routingContext) {
    LOG.error("Error while accessing route", routingContext.failure());
    routingContext.next();
  }
}
