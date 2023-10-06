package com.tngtech.keycloakmock.impl;

import static java.util.Optional.ofNullable;

import com.tngtech.keycloakmock.api.TokenConfig;
import com.tngtech.keycloakmock.impl.session.UserData;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import java.security.Key;
import java.security.PublicKey;
import java.util.Date;
import java.util.Map;
import javax.annotation.Nonnull;
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
    if (tokenConfig.isGenerateUserDataFromSubject()) {
      UserData generatedUserData =
          UserData.fromUsernameAndHostname(
              tokenConfig.getSubject(), requestConfiguration.getHostname());
      builder
          .claim("name", ofNullable(tokenConfig.getName()).orElse(generatedUserData.getName()))
          .claim(
              "given_name",
              ofNullable(tokenConfig.getGivenName()).orElse(generatedUserData.getGivenName()))
          .claim(
              "family_name",
              ofNullable(tokenConfig.getFamilyName()).orElse(generatedUserData.getFamilyName()))
          .claim("email", ofNullable(tokenConfig.getEmail()).orElse(generatedUserData.getEmail()))
          .claim(
              "preferred_username",
              ofNullable(tokenConfig.getPreferredUsername())
                  .orElse(generatedUserData.getPreferredUsername()));
    } else {
      builder
          .claim("name", tokenConfig.getName())
          .claim("given_name", tokenConfig.getGivenName())
          .claim("family_name", tokenConfig.getFamilyName())
          .claim("email", tokenConfig.getEmail())
          .claim("preferred_username", tokenConfig.getPreferredUsername());
    }
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
}
