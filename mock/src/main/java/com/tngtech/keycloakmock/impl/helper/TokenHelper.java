package com.tngtech.keycloakmock.impl.helper;

import static com.tngtech.keycloakmock.api.TokenConfig.aTokenConfig;

import com.tngtech.keycloakmock.api.TokenConfig.Builder;
import com.tngtech.keycloakmock.impl.TokenGenerator;
import com.tngtech.keycloakmock.impl.UrlConfiguration;
import com.tngtech.keycloakmock.impl.session.Session;
import com.tngtech.keycloakmock.impl.session.UserData;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class TokenHelper {

  private static final String SESSION_STATE = "session_state";
  private static final String NONCE = "nonce";

  @Nonnull private final TokenGenerator tokenGenerator;
  @Nonnull private final List<String> resourcesToMapRolesTo;
  @Nonnull private final Set<String> defaultScopes;

  @Nonnull private final Duration tokenLifespan;

  @Inject
  TokenHelper(
      @Nonnull TokenGenerator tokenGenerator,
      @Nonnull @Named("resources") List<String> resourcesToMapRolesTo,
      @Nonnull @Named("scopes") Set<String> defaultScopes,
      @Nonnull @Named("tokenLifespan") Duration tokenLifespan) {
    this.tokenGenerator = tokenGenerator;
    this.resourcesToMapRolesTo = resourcesToMapRolesTo;
    this.defaultScopes = defaultScopes;
    this.tokenLifespan = tokenLifespan;
  }

  @Nullable
  public String getToken(@Nonnull Session session, @Nonnull UrlConfiguration requestConfiguration) {
    UserData userData = session.getUserData();
    Builder builder =
        aTokenConfig()
            .withAuthorizedParty(session.getClientId())
            // at the moment, there is no explicit way of setting an audience
            .withAudience(session.getClientId())
            .withSubject(userData.getSubject())
            .withPreferredUsername(userData.getPreferredUsername())
            .withGivenName(userData.getGivenName())
            .withFamilyName(userData.getFamilyName())
            .withName(userData.getName())
            .withEmail(userData.getEmail())
            .withClaim(SESSION_STATE, session.getSessionId())
            // we currently don't do proper authorization anyway, so we can just act as if we were
            // compliant to ISO/IEC 29115 level 1 (see KEYCLOAK-3223 / KEYCLOAK-3314)
            .withAuthenticationContextClassReference("1")
            .witTokenLifespan(tokenLifespan);
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

    addDefaultScopesIfConfigured(builder);
    // for simplicity, the access token is the same as the ID token
    return tokenGenerator.getToken(builder.build(), requestConfiguration);
  }

  private void addDefaultScopesIfConfigured(Builder builder) {
    if (!defaultScopes.isEmpty()) {
      builder.withScopes(defaultScopes);
    }
  }

  @Nonnull
  public Map<String, Object> parseToken(@Nonnull String token) {
    return tokenGenerator.parseToken(token);
  }
}
