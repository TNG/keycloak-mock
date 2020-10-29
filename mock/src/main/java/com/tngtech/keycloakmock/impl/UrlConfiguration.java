package com.tngtech.keycloakmock.impl;

import com.tngtech.keycloakmock.api.ServerConfig;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class UrlConfiguration {
  private static final String ISSUER_PATH = "/auth/realms/";
  @Nonnull private final Protocol protocol;
  private final int port;
  @Nonnull private final String defaultHost;
  @Nonnull private final String defaultRealm;

  public UrlConfiguration(@Nonnull final ServerConfig serverConfig) {
    this.protocol = Objects.requireNonNull(serverConfig.getProtocol());
    this.port = serverConfig.getPort();
    if (protocol.getDefaultPort() == serverConfig.getPort()
        || serverConfig.getHostname().contains(":")) {
      this.defaultHost = serverConfig.getHostname();
    } else {
      this.defaultHost = serverConfig.getHostname() + ":" + serverConfig.getPort();
    }
    this.defaultRealm = Objects.requireNonNull(serverConfig.getRealm());
  }

  @Nonnull
  public String getBaseUrl(@Nullable final String requestHost) {
    return protocol.getValue() + (requestHost != null ? requestHost : defaultHost);
  }

  @Nonnull
  public String getIssuer(@Nullable final String requestHost, @Nullable final String requestRealm) {
    return getBaseUrl(requestHost)
        + ISSUER_PATH
        + (requestRealm != null ? requestRealm : defaultRealm);
  }

  @Nonnull
  public Protocol getProtocol() {
    return protocol;
  }

  public int getPort() {
    return port;
  }
}
