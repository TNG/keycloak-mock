package com.tngtech.keycloakmock.impl.handler;

import com.tngtech.keycloakmock.impl.UrlConfiguration;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.common.template.TemplateEngine;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class IFrameRoute implements Handler<RoutingContext> {

  private static final Logger LOG = LoggerFactory.getLogger(IFrameRoute.class);
  @Nonnull private final TemplateEngine engine;
  @Nonnull private final UrlConfiguration baseConfiguration;

  @Inject
  IFrameRoute(@Nonnull TemplateEngine engine, @Nonnull UrlConfiguration baseConfiguration) {
    this.engine = engine;
    this.baseConfiguration = baseConfiguration;
  }

  @Override
  public void handle(@Nonnull RoutingContext routingContext) {
    if (routingContext.normalizedPath().endsWith("/init")) {
      routingContext.response().setStatusCode(204).end();
      return;
    }
    routingContext.put("isSecureContext", routingContext.request().isSSL());
    routingContext.put(
        "resourceCommonUrl", baseConfiguration.forRequestContext(routingContext).getJsPath());
    engine
        .render(
            routingContext.data(), "/org/keycloak/protocol/oidc/endpoints/login-status-iframe.ftl")
        .onSuccess(
            b -> routingContext.response().putHeader(HttpHeaders.CONTENT_TYPE, "text/html").end(b))
        .onFailure(
            t -> {
              LOG.error("Unable to render login iframe", t);
              routingContext.fail(t);
            });
  }
}
