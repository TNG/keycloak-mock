package com.tngtech.keycloakmock.impl;

import static com.tngtech.keycloakmock.api.ServerConfig.DEFAULT_SCOPE;
import static com.tngtech.keycloakmock.api.TokenConfig.CLAIM_AUTHENTICATION_CONTEXT_REFERENCE;
import static com.tngtech.keycloakmock.api.TokenConfig.CLAIM_AUTHORIZED_PARTY;
import static com.tngtech.keycloakmock.api.TokenConfig.CLAIM_AUTH_TIME;
import static com.tngtech.keycloakmock.api.TokenConfig.CLAIM_EMAIL;
import static com.tngtech.keycloakmock.api.TokenConfig.CLAIM_FAMILY_NAME;
import static com.tngtech.keycloakmock.api.TokenConfig.CLAIM_GIVEN_NAME;
import static com.tngtech.keycloakmock.api.TokenConfig.CLAIM_NAME;
import static com.tngtech.keycloakmock.api.TokenConfig.CLAIM_PREFERRED_USERNAME;
import static com.tngtech.keycloakmock.api.TokenConfig.CLAIM_REALM_ACCESS;
import static com.tngtech.keycloakmock.api.TokenConfig.CLAIM_RESOURCE_ACCESS;
import static com.tngtech.keycloakmock.api.TokenConfig.CLAIM_SCOPE;
import static com.tngtech.keycloakmock.api.TokenConfig.CLAIM_SESSION_ID;
import static com.tngtech.keycloakmock.api.TokenConfig.CLAIM_SESSION_STATE;
import static com.tngtech.keycloakmock.api.TokenConfig.CLAIM_TYPE;
import static java.util.Optional.ofNullable;

import com.tngtech.keycloakmock.api.TokenConfig;
import com.tngtech.keycloakmock.impl.session.UserData;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import java.security.Key;
import java.security.PublicKey;
import java.time.Duration;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class TokenGenerator {
  @Nonnull private final PublicKey publicKey;
  @Nonnull private final Key privateKey;
  @Nonnull private final String keyId;
  @Nonnull private final Collection<String> defaultAudiences;
  @Nonnull private final Collection<String> defaultScopes;
  @Nonnull private final Duration defaultTokenLifespan;

  @Inject
  TokenGenerator(
      @Nonnull PublicKey publicKey,
      @Nonnull Key privateKey,
      @Nonnull @Named("keyId") String keyId,
      @Nonnull @Named("audiences") Collection<String> defaultAudiences,
      @Nonnull @Named("scopes") Collection<String> defaultScopes,
      @Nonnull @Named("tokenLifespan") Duration defaultTokenLifespan) {
    this.publicKey = publicKey;
    this.privateKey = privateKey;
    this.keyId = keyId;
    this.defaultScopes = defaultScopes;
    this.defaultAudiences = defaultAudiences;
    this.defaultTokenLifespan = defaultTokenLifespan;
  }

  @Nonnull
  public String getToken(
      @Nonnull TokenConfig tokenConfig, @Nonnull UrlConfiguration requestConfiguration) {
    JwtBuilder builder =
        Jwts.builder()
            .header()
            .keyId(keyId)
            .type("JWT")
            .and()
            .audience()
            .add(tokenConfig.getAudience().isEmpty() ? defaultAudiences : tokenConfig.getAudience())
            .and()
            .issuedAt(new Date(tokenConfig.getIssuedAt().toEpochMilli()))
            .claim(CLAIM_AUTH_TIME, tokenConfig.getAuthenticationTime().getEpochSecond())
            .issuer(requestConfiguration.getIssuer().toASCIIString())
            .subject(tokenConfig.getSubject())
            .claim(CLAIM_SCOPE, encodeGivenOrDefaultScopes(tokenConfig.getScopes()))
            .claim(CLAIM_TYPE, "Bearer")
            .claim(CLAIM_AUTHORIZED_PARTY, tokenConfig.getAuthorizedParty())
            .claim(CLAIM_SESSION_ID, tokenConfig.getSessionId())
            .claim(CLAIM_SESSION_STATE, tokenConfig.getSessionId());
    if (tokenConfig.getNotBefore() != null) {
      builder.notBefore(new Date(tokenConfig.getNotBefore().toEpochMilli()));
    }
    if (tokenConfig.getExpiration() != null) {
      builder.expiration(new Date(tokenConfig.getExpiration().toEpochMilli()));
    } else {
      builder.expiration(
          new Date(tokenConfig.getIssuedAt().plus(defaultTokenLifespan).toEpochMilli()));
    }
    if (tokenConfig.isGenerateUserDataFromSubject()) {
      UserData generatedUserData =
          UserData.fromUsernameAndHostname(
              tokenConfig.getSubject(), requestConfiguration.getHostname());
      builder
          .claim(CLAIM_NAME, ofNullable(tokenConfig.getName()).orElse(generatedUserData.getName()))
          .claim(
              CLAIM_GIVEN_NAME,
              ofNullable(tokenConfig.getGivenName()).orElse(generatedUserData.getGivenName()))
          .claim(
              CLAIM_FAMILY_NAME,
              ofNullable(tokenConfig.getFamilyName()).orElse(generatedUserData.getFamilyName()))
          .claim(
              CLAIM_EMAIL, ofNullable(tokenConfig.getEmail()).orElse(generatedUserData.getEmail()))
          .claim(
              CLAIM_PREFERRED_USERNAME,
              ofNullable(tokenConfig.getPreferredUsername())
                  .orElse(generatedUserData.getPreferredUsername()));
    } else {
      builder
          .claim(CLAIM_NAME, tokenConfig.getName())
          .claim(CLAIM_GIVEN_NAME, tokenConfig.getGivenName())
          .claim(CLAIM_FAMILY_NAME, tokenConfig.getFamilyName())
          .claim(CLAIM_EMAIL, tokenConfig.getEmail())
          .claim(CLAIM_PREFERRED_USERNAME, tokenConfig.getPreferredUsername());
    }
    return builder
        .claim(
            CLAIM_AUTHENTICATION_CONTEXT_REFERENCE,
            tokenConfig.getAuthenticationContextClassReference())
        .claim(CLAIM_REALM_ACCESS, tokenConfig.getRealmAccess())
        .claim(CLAIM_RESOURCE_ACCESS, tokenConfig.getResourceAccess())
        .claims()
        .add(tokenConfig.getClaims())
        .and()
        .signWith(privateKey)
        .compact();
  }

  private String encodeGivenOrDefaultScopes(List<String> scopes) {
    if (scopes.isEmpty()) {
      return Stream.concat(Stream.of(DEFAULT_SCOPE), defaultScopes.stream())
          .distinct()
          .collect(Collectors.joining(" "));
    } else {
      return Stream.concat(Stream.of(DEFAULT_SCOPE), scopes.stream())
          .distinct()
          .collect(Collectors.joining(" "));
    }
  }

  public Claims parseToken(@Nonnull String token) {
    JwtParser parser = Jwts.parser().verifyWith(publicKey).build();
    return parser.parseSignedClaims(token).getPayload();
  }
}
