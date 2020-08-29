package com.tngtech.keycloakmock.standalone.render;

import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.common.template.TemplateEngine;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RenderHelper {
  private static final Logger LOG = LoggerFactory.getLogger(RenderHelper.class);

  @Nonnull private final TemplateEngine engine;

  public RenderHelper(@Nonnull final TemplateEngine engine) {
    this.engine = engine;
  }

  public void renderTemplate(
      @Nonnull final RoutingContext routingContext,
      @Nonnull final String name,
      @Nonnull final String contentType) {
    engine.render(
        new JsonObject(routingContext.data()),
        name,
        res -> {
          if (res.succeeded()) {
            routingContext
                .response()
                .putHeader(HttpHeaders.CONTENT_TYPE, contentType)
                .end(res.result());
          } else {
            LOG.error("Unable to render template {}", name, res.cause());
            routingContext.fail(res.cause());
          }
        });
  }
}
