package com.tngtech.keycloakmock.impl;

import com.tngtech.keycloakmock.api.TokenConfig;
import com.tngtech.keycloakmock.impl.session.UserData;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import java.security.Key;
import java.security.PublicKey;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class TokenGenerator {
  @Nonnull private final PublicKey publicKey;
  @Nonnull private final Key privateKey;
  @Nonnull private final String keyId;

  @Inject
  TokenGenerator(
      @Nonnull PublicKey publicKey,
      @Nonnull Key privateKey,
      @Nonnull @Named("keyId") String keyId) {
    this.publicKey = publicKey;
    this.privateKey = privateKey;
    this.keyId = keyId;
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
            .add(tokenConfig.getAudience())
            .and()
            .issuedAt(new Date(tokenConfig.getIssuedAt().toEpochMilli()))
            .claim("auth_time", tokenConfig.getAuthenticationTime().getEpochSecond())
            .expiration(new Date(tokenConfig.getExpiration().toEpochMilli()))
            .issuer(
                requestConfiguration
                    .forRequestContext(tokenConfig.getHostname(), tokenConfig.getRealm())
                    .getIssuer()
                    .toASCIIString())
            .subject(tokenConfig.getSubject())
            .claim("scope", tokenConfig.getScope())
            .claim("typ", "Bearer")
            .claim("azp", tokenConfig.getAuthorizedParty());
    if (tokenConfig.getNotBefore() != null) {
      builder.notBefore(new Date(tokenConfig.getNotBefore().toEpochMilli()));
    }
    Optional<UserData> generatedUserData;
    if (tokenConfig.isGenerateUserDataFromSubject()) {
      generatedUserData =
          Optional.of(
              UserData.fromUsernameAndHostname(
                  tokenConfig.getSubject(), requestConfiguration.getHostname()));
    } else {
      generatedUserData = Optional.empty();
    }
    setClaimIfPresent(
        builder,
        "name",
        tokenConfig.getName(),
        generatedUserData.map(UserData::getName).orElse(null));
    setClaimIfPresent(
        builder,
        "given_name",
        tokenConfig.getGivenName(),
        generatedUserData.map(UserData::getGivenName).orElse(null));
    setClaimIfPresent(
        builder,
        "family_name",
        tokenConfig.getFamilyName(),
        generatedUserData.map(UserData::getFamilyName).orElse(null));
    setClaimIfPresent(
        builder,
        "email",
        tokenConfig.getEmail(),
        generatedUserData.map(UserData::getEmail).orElse(null));
    setClaimIfPresent(
        builder,
        "preferred_username",
        tokenConfig.getPreferredUsername(),
        generatedUserData.map(UserData::getPreferredUsername).orElse(null));
    return builder
        .claim("acr", tokenConfig.getAuthenticationContextClassReference())
        .claim("realm_access", tokenConfig.getRealmAccess())
        .claim("resource_access", tokenConfig.getResourceAccess())
        .claims()
        .add(tokenConfig.getClaims())
        .and()
        .signWith(privateKey)
        .compact();
  }

  public Map<String, Object> parseToken(String token) {
    JwtParser parser = Jwts.parser().verifyWith(publicKey).build();
    return parser.parseSignedClaims(token).getPayload();
  }

  private void setClaimIfPresent(
      @Nonnull final JwtBuilder builder,
      @Nonnull final String claim,
      @Nullable String value,
      @Nullable String alternative) {
    if (value != null) {
      Objects.requireNonNull(builder).claim(claim, value);
    } else if (alternative != null) {
      Objects.requireNonNull(builder).claim(claim, alternative);
    }
  }
}
