package com.tngtech.keycloakmock.impl.handler;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.TEXT_HTML;

import io.vertx.core.Handler;
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
        .onSuccess(b -> routingContext.response().putHeader(CONTENT_TYPE, TEXT_HTML).end(b))
        .onFailure(
            t -> {
              LOG.error("Unable to render oob page", t);
              routingContext.fail(t);
            });
  }
}
