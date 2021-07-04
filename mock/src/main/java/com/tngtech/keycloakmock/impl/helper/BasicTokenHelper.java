package com.tngtech.keycloakmock.impl.helper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;

/** Helper to handle Keycloak base64 basic token. */
public class BasicTokenHelper {

  @Nonnull
  public Map<String, String> parseToken(@Nonnull String token) {
    final Map<String, String> clientCredentials = new HashMap<>();
    if (!token.contains("Basic")) {
      return clientCredentials;
    }
    final String sanitizedToken = token.replace("Basic", "").trim();
    final String decodedToken =
        new String(Base64.getDecoder().decode(sanitizedToken), StandardCharsets.UTF_8);

    // Keycloak basic authorization token format is : clientId:clientSecret
    if (decodedToken.contains(":")) {
      clientCredentials.put("clientId", decodedToken.split(":")[0]);
      clientCredentials.put("clientSecret", decodedToken.split(":")[1]);
    }
    return clientCredentials;
  }
}
