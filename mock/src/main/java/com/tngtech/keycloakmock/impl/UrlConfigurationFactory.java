package com.tngtech.keycloakmock.impl;

import com.tngtech.keycloakmock.api.ServerConfig;
import io.vertx.ext.web.RoutingContext;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class UrlConfigurationFactory {
  @Nonnull private final ServerConfig serverConfig;
  private volatile int actualPort;

  public UrlConfigurationFactory(@Nonnull ServerConfig serverConfig) {
    this.serverConfig = serverConfig;
  }

  /**
   * Set the port the server actually bound to.
   *
   * <p>Needed when the server was configured with a random port, as {@link ServerConfig#getPort()}
   * only contains the configured value (i.e. {@code 0}) in that case.
   *
   * @param actualPort the port the server is listening on
   */
  public void setActualPort(int actualPort) {
    this.actualPort = actualPort;
  }

  /** Clear the actual port, falling back to the configured port again. */
  public void clearActualPort() {
    this.actualPort = 0;
  }

  public UrlConfiguration create(@Nullable String requestHost, @Nullable String requestRealm) {
    return new UrlConfiguration(serverConfig, getEffectivePort(), requestHost, requestRealm);
  }

  public UrlConfiguration create(@Nonnull RoutingContext routingContext) {
    return new UrlConfiguration(
        serverConfig,
        getEffectivePort(),
        routingContext.request().getHeader("Host"),
        routingContext.pathParam("realm"));
  }

  private int getEffectivePort() {
    int port = actualPort;
    return port > 0 ? port : serverConfig.getPort();
  }
}
