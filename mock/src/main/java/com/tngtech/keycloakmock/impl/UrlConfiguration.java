package com.tngtech.keycloakmock.impl;

import com.tngtech.keycloakmock.api.ServerConfig;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class UrlConfiguration {
  private static final String ISSUER_PATH = "/realms/";
  private static final String AUTHENTICATION_CALLBACK_PATH = "authenticate/";
  private static final String OUT_OF_BAND_PATH = "oob";
  private static final String ISSUER_OPEN_ID_PATH = "protocol/openid-connect/";
  private static final String OPEN_ID_TOKEN_PATH = "token";
  private static final String OPEN_ID_JWKS_PATH = "certs";
  private static final String OPEN_ID_AUTHORIZATION_PATH = "auth";
  private static final String OPEN_ID_END_SESSION_PATH = "logout";
  @Nonnull private final Protocol protocol;
  private final int port;
  @Nonnull private final String hostname;
  @Nonnull private final String contextPath;
  @Nonnull private final String realm;

  public UrlConfiguration(@Nonnull final ServerConfig serverConfig) {
    this.protocol = Objects.requireNonNull(serverConfig.getProtocol());
    this.port = serverConfig.getPort();
    if (protocol.getDefaultPort() == serverConfig.getPort()
        || serverConfig.getDefaultHostname().contains(":")) {
      this.hostname = serverConfig.getDefaultHostname();
    } else {
      this.hostname = serverConfig.getDefaultHostname() + ":" + serverConfig.getPort();
    }
    if (Objects.requireNonNull(serverConfig.getContextPath()).isEmpty()) {
      this.contextPath = serverConfig.getContextPath();
    } else {
      this.contextPath =
          serverConfig.getContextPath().startsWith("/")
              ? serverConfig.getContextPath()
              : "/".concat(serverConfig.getContextPath());
    }
    this.realm = Objects.requireNonNull(serverConfig.getDefaultRealm());
  }

  private UrlConfiguration(
      @Nonnull final UrlConfiguration baseConfiguration,
      @Nullable final String requestHost,
      @Nullable final String requestRealm) {
    this.protocol = baseConfiguration.protocol;
    this.port = baseConfiguration.port;
    this.hostname = requestHost != null ? requestHost : baseConfiguration.hostname;
    this.contextPath = baseConfiguration.contextPath;
    this.realm = requestRealm != null ? requestRealm : baseConfiguration.realm;
  }

  @Nonnull
  public UrlConfiguration forRequestContext(
      @Nullable final String requestHost, @Nullable final String requestRealm) {
    return new UrlConfiguration(this, requestHost, requestRealm);
  }

  @Nonnull
  URI getBaseUrl() {
    try {
      return new URI(protocol.getValue() + hostname);
    } catch (URISyntaxException e) {
      throw new IllegalStateException("Invalid URL encountered", e);
    }
  }

  @Nonnull
  public URI getIssuer() {
    return getBaseUrl().resolve(contextPath + ISSUER_PATH + realm);
  }

  @Nonnull
  public URI getIssuerPath() {
    return getBaseUrl().resolve(contextPath + ISSUER_PATH + realm + "/");
  }

  @Nonnull
  public URI getOpenIdPath(@Nonnull final String path) {
    return getIssuerPath().resolve(ISSUER_OPEN_ID_PATH).resolve(path);
  }

  @Nonnull
  public URI getAuthenticationCallbackEndpoint(@Nonnull final String sessionId) {
    return getIssuerPath().resolve(AUTHENTICATION_CALLBACK_PATH + sessionId);
  }

  @Nonnull
  public URI getOutOfBandLoginLoginEndpoint() {
    return getIssuerPath().resolve(OUT_OF_BAND_PATH);
  }

  @Nonnull
  public URI getAuthorizationEndpoint() {
    return getOpenIdPath(OPEN_ID_AUTHORIZATION_PATH);
  }

  @Nonnull
  public URI getEndSessionEndpoint() {
    return getOpenIdPath(OPEN_ID_END_SESSION_PATH);
  }

  @Nonnull
  public URI getTokenEndpoint() {
    return getOpenIdPath(OPEN_ID_TOKEN_PATH);
  }

  @Nonnull
  public URI getJwksUri() {
    return getOpenIdPath(OPEN_ID_JWKS_PATH);
  }

  @Nonnull
  public Protocol getProtocol() {
    return protocol;
  }

  public int getPort() {
    return port;
  }

  @Nonnull
  public String getHostname() {
    return hostname;
  }

  @Nonnull
  public String getRealm() {
    return realm;
  }
}
