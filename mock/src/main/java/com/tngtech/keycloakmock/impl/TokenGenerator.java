package com.tngtech.keycloakmock.impl;

import com.tngtech.keycloakmock.api.TokenConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
    return getToken(tokenConfig, requestConfiguration, new HashSet<>());
  }

  @Nonnull
  public String getToken(
      @Nonnull TokenConfig tokenConfig,
      @Nonnull UrlConfiguration requestConfiguration,
      @Nonnull Set<String> userAliases) {
    JwtBuilder builder =
        Jwts.builder()
            .setHeaderParam("kid", keyId)
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
    setClaimIfPresent(builder, "nbf", tokenConfig.getNotBefore());
    setClaimIfPresent(builder, "name", tokenConfig.getName());
    setClaimIfPresent(builder, "given_name", tokenConfig.getGivenName());
    setClaimIfPresent(builder, "family_name", tokenConfig.getFamilyName());
    setClaimIfPresent(builder, "email", tokenConfig.getEmail());
    setClaimIfPresent(builder, "preferred_username", tokenConfig.getPreferredUsername());
    setClaimIfPresent(builder, "acr", tokenConfig.getAuthenticationContextClassReference());

    for (String alias : userAliases) {
      setClaimIfPresent(builder, alias, tokenConfig.getPreferredUsername());
    }

    return builder
        .claim("realm_access", tokenConfig.getRealmAccess())
        .claim("resource_access", tokenConfig.getResourceAccess())
        .addClaims(tokenConfig.getClaims())
        .signWith(privateKey, algorithm)
        .compact();
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> parseToken(String token) {
    JwtParser parser = Jwts.parserBuilder().setSigningKey(privateKey).build();
    return ((Jwt<Header<?>, Claims>) parser.parse(token)).getBody();
  }

  private void setClaimIfPresent(
      @Nonnull final JwtBuilder builder, @Nonnull final String claim, @Nullable String value) {
    if (value != null) {
      Objects.requireNonNull(builder).claim(claim, value);
    }
  }

  private void setClaimIfPresent(
      @Nonnull final JwtBuilder builder, @Nonnull final String claim, @Nullable Instant value) {
    if (value != null) {
      Objects.requireNonNull(builder).claim(claim, value.getEpochSecond());
    }
  }
}
