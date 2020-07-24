package com.tngtech.keycloakmock.standalone.handler;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FailureHandler implements Handler<RoutingContext> {
  private static final Logger LOG = LoggerFactory.getLogger(FailureHandler.class);

  @Override
  public void handle(RoutingContext routingContext) {
    LOG.error("Error while accessing route", routingContext.failure());
  }
}
