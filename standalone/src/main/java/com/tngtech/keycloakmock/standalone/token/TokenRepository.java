package com.tngtech.keycloakmock.standalone.token;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

public class TokenRepository {
  private final Map<String, String> tokens = new HashMap<>();

  @Nullable
  public String getToken(String authorizationCode) {
    return tokens.get(authorizationCode);
  }

  public void putToken(String authorizationCode, String token) {
    tokens.put(authorizationCode, token);
  }
}
