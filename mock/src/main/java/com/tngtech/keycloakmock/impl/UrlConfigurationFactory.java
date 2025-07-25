package com.tngtech.keycloakmock.impl;

import com.tngtech.keycloakmock.api.ServerConfig;
import io.vertx.ext.web.RoutingContext;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class UrlConfigurationFactory {
  @Nonnull private final ServerConfig serverConfig;

  @Inject
  public UrlConfigurationFactory(@Nonnull ServerConfig serverConfig) {
    this.serverConfig = serverConfig;
  }

  public UrlConfiguration create(@Nullable String requestHost, @Nullable String requestRealm) {
    return new UrlConfiguration(serverConfig, requestHost, requestRealm);
  }

  public UrlConfiguration create(@Nonnull RoutingContext routingContext) {
    return new UrlConfiguration(
        serverConfig,
        routingContext.request().getHeader("Host"),
        routingContext.pathParam("realm"));
  }
}
