package com.tngtech.keycloakmock.standalone.token;

import com.tngtech.keycloakmock.api.TokenConfig;
import javax.annotation.Nonnull;

public interface TokenFactory {
  @Nonnull
  String getToken(
      @Nonnull final TokenConfig config,
      @Nonnull final String hostname,
      @Nonnull final String realm);
}
