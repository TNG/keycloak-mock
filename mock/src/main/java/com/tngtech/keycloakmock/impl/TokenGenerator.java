package com.tngtech.keycloakmock.impl;

import com.tngtech.keycloakmock.api.TokenConfig;
import com.tngtech.keycloakmock.impl.session.UserData;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.security.Key;
import java.time.Instant;
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
  @Nonnull private final Key privateKey;
  @Nonnull private final String keyId;
  @Nonnull private final SignatureAlgorithm algorithm;

  @Inject
  TokenGenerator(
      @Nonnull Key privateKey,
      @Nonnull @Named("keyId") String keyId,
      @Nonnull SignatureAlgorithm algorithm) {
    this.privateKey = privateKey;
    this.keyId = keyId;
    this.algorithm = algorithm;
  }

  @Nonnull
  public String getToken(
      @Nonnull TokenConfig tokenConfig, @Nonnull UrlConfiguration requestConfiguration) {
    JwtBuilder builder =
        Jwts.builder()
            .setHeaderParam("alg", algorithm.getValue())
            .setHeaderParam("kid", keyId)
            .setHeaderParam("typ", "JWT")
            // since the specification allows for more than one audience, but JJWT only accepts
            // one (see https://github.com/jwtk/jjwt/issues/77), use a workaround here
            .claim("aud", tokenConfig.getAudience())
            .setIssuedAt(new Date(tokenConfig.getIssuedAt().toEpochMilli()))
            .claim("auth_time", tokenConfig.getAuthenticationTime().getEpochSecond())
            .setExpiration(new Date(tokenConfig.getExpiration().toEpochMilli()))
            .setIssuer(
                requestConfiguration
                    .forRequestContext(tokenConfig.getHostname(), tokenConfig.getRealm())
                    .getIssuer()
                    .toASCIIString())
            .setSubject(tokenConfig.getSubject())
            .claim("scope", tokenConfig.getScope())
            .claim("typ", "Bearer")
            .claim("azp", tokenConfig.getAuthorizedParty());
    Optional<UserData> generatedUserData;
    if (tokenConfig.isGenerateUserDataFromSubject()) {
      generatedUserData =
          Optional.of(
              UserData.fromUsernameAndHostname(
                  tokenConfig.getSubject(), requestConfiguration.getHostname()));
    } else {
      generatedUserData = Optional.empty();
    }
    setClaimIfPresent(builder, "nbf", tokenConfig.getNotBefore());
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
    setClaimIfPresent(builder, "acr", tokenConfig.getAuthenticationContextClassReference());
    return builder
        .claim("realm_access", tokenConfig.getRealmAccess())
        .claim("resource_access", tokenConfig.getResourceAccess())
        .addClaims(tokenConfig.getClaims())
        .signWith(privateKey, algorithm)
        .compact();
  }

  public Map<String, Object> parseToken(String token) {
    JwtParser parser = Jwts.parserBuilder().setSigningKey(privateKey).build();
    return parser.parseClaimsJws(token).getBody();
  }

  private void setClaimIfPresent(
      @Nonnull final JwtBuilder builder, @Nonnull final String claim, @Nullable String value) {
    if (value != null) {
      Objects.requireNonNull(builder).claim(claim, value);
    }
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

  private void setClaimIfPresent(
      @Nonnull final JwtBuilder builder, @Nonnull final String claim, @Nullable Instant value) {
    if (value != null) {
      Objects.requireNonNull(builder).claim(claim, value.getEpochSecond());
    }
  }
}
