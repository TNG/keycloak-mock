package com.tngtech.keycloakmock.impl.handler;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.common.template.TemplateEngine;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DelegationRoute implements Handler<RoutingContext> {
  private static final Logger LOG = LoggerFactory.getLogger(DelegationRoute.class);
  private static final String HEADER = "header";
  private static final String BODY = "body";

  @Nonnull private final TemplateEngine engine;

  public DelegationRoute(@Nonnull TemplateEngine engine) {
    this.engine = engine;
  }

  @Override
  public void handle(RoutingContext routingContext) {
    if ("true".equals(routingContext.queryParams().get("error"))) {
      routingContext.put(HEADER, "Delegation failed");
      routingContext.put(
          BODY, "You need to check the output of your client to see what went wrong.");
    } else {
      routingContext.put(HEADER, "Delegation successful");
      routingContext.put(BODY, "You may now close this browser window.");
    }
    engine
        .render(routingContext.data(), "delegation.ftl")
        .onSuccess(
            b -> routingContext.response().putHeader(HttpHeaders.CONTENT_TYPE, "text/html").end(b))
        .onFailure(
            t -> {
              LOG.error("Unable to render login page", t);
              routingContext.fail(t);
            });
  }
}
