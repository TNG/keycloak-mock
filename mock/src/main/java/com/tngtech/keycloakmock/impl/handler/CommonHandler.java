package com.tngtech.keycloakmock.impl.handler;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommonHandler implements Handler<RoutingContext> {
  private static final Logger LOG = LoggerFactory.getLogger(CommonHandler.class);

  @Override
  public void handle(@Nonnull final RoutingContext routingContext) {
    routingContext
        .response()
        .bodyEndHandler(
            aVoid ->
                LOG.info(
                    "{}: {} {}",
                    routingContext.response().getStatusCode(),
                    routingContext.request().method().name(),
                    routingContext.request().uri()))
        .putHeader("Access-Control-Allow-Origin", routingContext.request().headers().get("Origin"))
        .putHeader("Access-Control-Allow-Methods", "GET,POST")
        .putHeader("Access-Control-Allow-Credentials", "true");
    routingContext.next();
  }
}
