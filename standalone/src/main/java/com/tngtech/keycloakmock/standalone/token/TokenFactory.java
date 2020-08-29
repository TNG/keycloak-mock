package com.tngtech.keycloakmock.standalone.token;

import com.tngtech.keycloakmock.api.TokenConfig;
import javax.annotation.Nonnull;

public interface TokenFactory {
  @Nonnull
  String getToken(TokenConfig config, String hostname, String realm);
}
