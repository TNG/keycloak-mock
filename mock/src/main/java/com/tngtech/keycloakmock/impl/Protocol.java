package com.tngtech.keycloakmock.impl;

import javax.annotation.Nonnull;

public enum Protocol {
  HTTP("http://", false, 80),
  HTTPS("https://", true, 443);

  @Nonnull private final String value;
  private final boolean tls;
  private final int defaultPort;

  Protocol(@Nonnull String value, boolean tls, int defaultPort) {
    this.value = value;
    this.tls = tls;
    this.defaultPort = defaultPort;
  }

  @Nonnull
  public String getValue() {
    return value;
  }

  public boolean isTls() {
    return tls;
  }

  public int getDefaultPort() {
    return defaultPort;
  }
}
