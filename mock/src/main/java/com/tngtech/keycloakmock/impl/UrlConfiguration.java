package com.tngtech.keycloakmock.impl;

import com.tngtech.keycloakmock.api.ServerConfig;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class UrlConfiguration {
  private static final String AUTHORIZATION_PATH = "/authenticate";
  private static final String END_SESSION_PATH = "/logout";
  private static final String ISSUER_PATH = "/auth/realms/";
  private static final String ISSUER_TOKEN_PATH = "/protocol/openid-connect/token";
  private static final String ISSUER_JWKS_PATH = "/protocol/openid-connect/certs";
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
  public String getAuthorizationEndpoint(@Nullable final String requestHost) {
    return getBaseUrl(requestHost) + AUTHORIZATION_PATH;
  }

  @Nonnull
  public String getEndSessionEndpoint(@Nullable final String requestHost) {
    return getBaseUrl(requestHost) + END_SESSION_PATH;
  }

  @Nonnull
  public String getIssuer(@Nullable final String requestHost, @Nullable final String requestRealm) {
    return getBaseUrl(requestHost)
        + ISSUER_PATH
        + (requestRealm != null ? requestRealm : defaultRealm);
  }

  @Nonnull
  public String getTokenEndpoint(
      @Nullable final String requestHost, @Nullable final String requestRealm) {
    return getIssuer(requestHost, requestRealm) + ISSUER_TOKEN_PATH;
  }

  @Nonnull
  public String getJwksUri(
      @Nullable final String requestHost, @Nullable final String requestRealm) {
    return getIssuer(requestHost, requestRealm) + ISSUER_JWKS_PATH;
  }

  @Nonnull
  public Protocol getProtocol() {
    return protocol;
  }

  public int getPort() {
    return port;
  }
}
