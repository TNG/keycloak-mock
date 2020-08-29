package com.tngtech.keycloakmock.standalone.token;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TokenRepository {
  @Nonnull private final Map<String, String> tokens = new HashMap<>();

  @Nullable
  public String getToken(@Nonnull final String authorizationCode) {
    return tokens.get(authorizationCode);
  }

  public void putToken(@Nonnull final String authorizationCode, @Nonnull final String token) {
    tokens.put(authorizationCode, token);
  }
}
