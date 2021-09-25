package com.tngtech.keycloakmock.impl.handler;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.common.template.TemplateEngine;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class OutOfBandLoginRoute implements Handler<RoutingContext> {
  private static final Logger LOG = LoggerFactory.getLogger(OutOfBandLoginRoute.class);
  private static final String CODE = "code";

  @Nonnull private final TemplateEngine engine;

  @Inject
  OutOfBandLoginRoute(@Nonnull TemplateEngine engine) {
    this.engine = engine;
  }

  @Override
  public void handle(@Nonnull RoutingContext routingContext) {
    routingContext.put(
        CODE, Optional.ofNullable(routingContext.queryParams().get(CODE)).orElse("invalid"));
    engine
        .render(routingContext.data(), "oob.ftl")
        .onSuccess(
            b -> routingContext.response().putHeader(HttpHeaders.CONTENT_TYPE, "text/html").end(b))
        .onFailure(
            t -> {
              LOG.error("Unable to render oob page", t);
              routingContext.fail(t);
            });
  }
}
