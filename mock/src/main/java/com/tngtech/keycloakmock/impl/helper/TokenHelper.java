package com.tngtech.keycloakmock.impl.helper;

import static com.tngtech.keycloakmock.api.TokenConfig.aTokenConfig;

import com.tngtech.keycloakmock.api.LoginRoleMapping;
import com.tngtech.keycloakmock.api.TokenConfig.Builder;
import com.tngtech.keycloakmock.impl.TokenGenerator;
import com.tngtech.keycloakmock.impl.UrlConfiguration;
import com.tngtech.keycloakmock.impl.session.Session;
import com.tngtech.keycloakmock.impl.session.UserData;
import java.util.Collection;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class TokenHelper {

  private static final String NONCE = "nonce";

  @Nonnull private final TokenGenerator tokenGenerator;
  @Nonnull private final Collection<String> defaultAudiences;
  @Nonnull private final LoginRoleMapping loginRoleMapping;

  @Inject
  TokenHelper(
      @Nonnull TokenGenerator tokenGenerator,
      @Nonnull @Named("audiences") Collection<String> defaultAudiences,
      @Nonnull LoginRoleMapping loginRoleMapping) {
    this.tokenGenerator = tokenGenerator;
    this.defaultAudiences = defaultAudiences;
    this.loginRoleMapping = loginRoleMapping;
  }

  @Nullable
  public String getToken(@Nonnull Session session, @Nonnull UrlConfiguration requestConfiguration) {
    UserData userData = session.getUserData();
    Builder builder =
        aTokenConfig()
            .withAuthorizedParty(session.getClientId())
            .withAudience(session.getClientId())
            .withAudiences(defaultAudiences)
            .withSubject(userData.getSubject())
            .withPreferredUsername(userData.getPreferredUsername())
            .withGivenName(userData.getGivenName())
            .withFamilyName(userData.getFamilyName())
            .withName(userData.getName())
            .withEmail(userData.getEmail())
            .withSessionId(session.getSessionId())
            // we currently don't do proper authorization anyway, so we can just act as if we were
            // compliant to ISO/IEC 29115 level 1 (see KEYCLOAK-3223 / KEYCLOAK-3314)
            .withAuthenticationContextClassReference("1");
    if (session.getNonce() != null) {
      builder.withClaim(NONCE, session.getNonce());
    }
    switch (loginRoleMapping) {
      case TO_REALM:
        builder.withRealmRoles(session.getRoles());
        break;
      case TO_RESOURCE:
        setResourceRoles(builder, session);
        break;
      case TO_BOTH:
        builder.withRealmRoles(session.getRoles());
        setResourceRoles(builder, session);
        break;
    }

    // for simplicity, the access token is the same as the ID token
    return tokenGenerator.getToken(builder.build(), requestConfiguration);
  }

  private void setResourceRoles(@Nonnull Builder builder, @Nonnull Session session) {
    // we always set the client ID as audience, so we also need to set the roles
    builder.withResourceRoles(session.getClientId(), session.getRoles());
    for (String audience : defaultAudiences) {
      builder.withResourceRoles(audience, session.getRoles());
    }
  }

  @Nonnull
  public Map<String, Object> parseToken(@Nonnull String token) {
    return tokenGenerator.parseToken(token);
  }
}
