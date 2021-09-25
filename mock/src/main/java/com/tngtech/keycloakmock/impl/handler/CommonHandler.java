package com.tngtech.keycloakmock.impl.handler;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class CommonHandler implements Handler<RoutingContext> {
  private static final Logger LOG = LoggerFactory.getLogger(CommonHandler.class);

  @Inject
  CommonHandler() {}

  @Override
  public void handle(@Nonnull RoutingContext routingContext) {
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
