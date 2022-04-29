package com.tngtech.keycloakmock.impl.helper;

import static com.tngtech.keycloakmock.api.TokenConfig.aTokenConfig;

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;

import com.tngtech.keycloakmock.api.TokenConfig.Builder;
import com.tngtech.keycloakmock.impl.TokenGenerator;
import com.tngtech.keycloakmock.impl.UrlConfiguration;
import com.tngtech.keycloakmock.impl.session.Session;

@Singleton
public class TokenHelper {

  private static final String SESSION_STATE = "session_state";
  private static final String NONCE = "nonce";

  @Nonnull private final TokenGenerator tokenGenerator;
  @Nonnull private final List<String> resourcesToMapRolesTo;

  @Inject
  TokenHelper(
      @Nonnull TokenGenerator tokenGenerator,
      @Nonnull @Named("resources") List<String> resourcesToMapRolesTo) {
    this.tokenGenerator = tokenGenerator;
    this.resourcesToMapRolesTo = resourcesToMapRolesTo;
  }

  @Nullable
  public String getToken(@Nonnull Session session, @Nonnull UrlConfiguration requestConfiguration) {
    Builder builder =
        aTokenConfig()
            .withAuthorizedParty(session.getClientId())
            // at the moment, there is no explicit way of setting an audience
            .withAudience(session.getClientId())
            .withClaim(SESSION_STATE, session.getSessionId())
            // we currently don't do proper authorization anyway, so we can just act as if we were
            // compliant to ISO/IEC 29115 level 1 (see KEYCLOAK-3223 / KEYCLOAK-3314)
            .withAuthenticationContextClassReference("1");
    resolveUsername(builder, session.getUsername());
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

  // To get an email, given name, and family name, use this email pattern as
  // the username when logging in:
  //
  //    firstname.lastname@somewhere.com
  //
  // It will result in these being set:
  //
  //    subject:           firstname.lastname
  //    preferredUsername: firstname.lastname
  //    givenName:         Firstname             // Capitalized
  //    familyName:        Lastname              // Capitalized
  //    email:             firstname.lastname@somewhere.com
  //
  // If the login user name is not an email, the provided value will be stored
  // as is everywhere, except for email and given name, which won't be set.
  private static void resolveUsername(Builder builder, String formUsername) {
    String cleanFormUsername = StringUtils.trimToNull(formUsername);
    if (cleanFormUsername == null) {
      return;
    }

    String username = StringUtils.substringBefore(cleanFormUsername, "@");
    builder.withSubject(username);
    builder.withPreferredUsername(username);

    // at this point if username is different than login user name,
    // we assume the supplied login user name is an email and we can
    // derive names
    if (!username.equals(cleanFormUsername)) {
        builder.withEmail(cleanFormUsername);
        builder.withGivenName(cap(StringUtils.substringBefore(username, ".")));
        builder.withFamilyName(cap(StringUtils.substringAfter(username, ".")));
    } else {
        builder.withFamilyName(username);
    }
  }
  private static String cap(String str) {
    if (str.isEmpty()) {
      return str;
    }
    return str.substring(0, 1).toUpperCase() + str.substring(1);
  }
}

