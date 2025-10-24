package com.tngtech.keycloakmock.impl.handler;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.TEXT_HTML;

import com.tngtech.keycloakmock.impl.UrlConfiguration;
import com.tngtech.keycloakmock.impl.UrlConfigurationFactory;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.common.template.TemplateEngine;
import java.net.URI;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class IFrameRoute implements Handler<RoutingContext> {

  private static final Logger LOG = LoggerFactory.getLogger(IFrameRoute.class);
  @Nonnull private final TemplateEngine engine;
  @Nonnull private final UrlConfigurationFactory urlConfigurationFactory;

  @Inject
  IFrameRoute(
      @Nonnull TemplateEngine engine, @Nonnull UrlConfigurationFactory urlConfigurationFactory) {
    this.engine = engine;
    this.urlConfigurationFactory = urlConfigurationFactory;
  }

  @Override
  public void handle(@Nonnull RoutingContext routingContext) {
    if (routingContext.normalizedPath().endsWith("/init")) {
      routingContext.response().setStatusCode(204).end();
      return;
    }
    routingContext.put("isSecureContext", routingContext.request().isSSL());
    routingContext.put("resourceCommonUrl", urlConfigurationFactory.create(routingContext).getJs());
    engine
        .render(
            routingContext.data(), "/org/keycloak/protocol/oidc/endpoints/login-status-iframe.ftl")
        .onSuccess(b -> routingContext.response().putHeader(CONTENT_TYPE, TEXT_HTML).end(b))
        .onFailure(
            t -> {
              LOG.error("Unable to render login iframe", t);
              routingContext.fail(t);
            });
  }

  public static URI getWebCryptoShimPath(@Nonnull UrlConfiguration urlConfiguration) {
    return urlConfiguration.getJsPath().resolve("vendor/web-crypto-shim/web-crypto-shim.js");
  }
}
