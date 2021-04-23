package com.tngtech.keycloakmock.impl.helper;

import static com.tngtech.keycloakmock.api.TokenConfig.aTokenConfig;

import com.tngtech.keycloakmock.api.TokenConfig.Builder;
import com.tngtech.keycloakmock.impl.TokenGenerator;
import com.tngtech.keycloakmock.impl.UrlConfiguration;
import com.tngtech.keycloakmock.impl.session.Session;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TokenHelper {

  private static final String SESSION_STATE = "session_state";
  private static final String NONCE = "nonce";

  @Nonnull private final TokenGenerator tokenGenerator;
  @Nonnull private final List<String> resourcesToMapRolesTo;

  public TokenHelper(
      @Nonnull TokenGenerator tokenGenerator, @Nonnull List<String> resourcesToMapRolesTo) {
    this.tokenGenerator = tokenGenerator;
    this.resourcesToMapRolesTo = resourcesToMapRolesTo;
  }

  @Nullable
  public String getToken(Session session, UrlConfiguration requestConfiguration) {
    Builder builder =
        aTokenConfig()
            .withAuthorizedParty(session.getClientId())
            // at the moment, there is no explicit way of setting an audience
            .withAudience(session.getClientId())
            .withSubject(session.getUsername())
            .withPreferredUsername(session.getUsername())
            .withFamilyName(session.getUsername())
            .withClaim(SESSION_STATE, session.getSessionId())
            // we currently don't do proper authorization anyway, so we can just act as if we were
            // compliant to ISO/IEC 29115 level 1 (see KEYCLOAK-3223 / KEYCLOAK-3314)
            .withAuthenticationContextClassReference("1");
    if (session.getNonce() != null) {
      builder.withClaim(NONCE, session.getNonce());
    }
    if (resourcesToMapRolesTo.isEmpty()) {
      builder.withRealmRoles(session.getRoles());
    } else {
      for (String resource : resourcesToMapRolesTo) {
        builder.withResourceRoles(resource, session.getRoles());
      }
    }
    // for simplicity, the access token is the same as the ID token
    return tokenGenerator.getToken(builder.build(), requestConfiguration);
  }

  @Nonnull
  public Map<String, Object> parseToken(@Nonnull String token) {
    return tokenGenerator.parseToken(token);
  }
}
