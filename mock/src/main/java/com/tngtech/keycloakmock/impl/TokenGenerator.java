package com.tngtech.keycloakmock.impl;

import com.tngtech.keycloakmock.api.TokenConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.io.IOException;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TokenGenerator {
  private static final String KEY_ID = "keyId";
  private static final SignatureAlgorithm ALGORITHM = SignatureAlgorithm.RS256;
  private static final String KEY = "rsa";

  @Nonnull private final Key privateKey;
  @Nonnull private final PublicKey publicKey;

  public TokenGenerator() {
    try {
      KeyStore keyStore = KeyStore.getInstance("JKS");
      try (InputStream keystoreStream = this.getClass().getResourceAsStream("/keystore.jks")) {
        keyStore.load(keystoreStream, null);
      }
      privateKey = Objects.requireNonNull(keyStore.getKey(KEY, new char[] {}));
      publicKey = Objects.requireNonNull(keyStore.getCertificate(KEY).getPublicKey());
    } catch (IOException
        | CertificateException
        | NoSuchAlgorithmException
        | UnrecoverableKeyException
        | KeyStoreException e) {
      throw new IllegalStateException("Error while loading keystore for signing token", e);
    }
  }

  @Nonnull
  public String getToken(
      @Nonnull final TokenConfig tokenConfig, @Nonnull UrlConfiguration requestConfiguration) {
    JwtBuilder builder =
        Jwts.builder()
            .setHeaderParam("kid", KEY_ID)
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
    return builder
        .claim("realm_access", tokenConfig.getRealmAccess())
        .claim("resource_access", tokenConfig.getResourceAccess())
        .addClaims(tokenConfig.getClaims())
        .signWith(privateKey, ALGORITHM)
        .compact();
  }

  @Nonnull
  public PublicKey getPublicKey() {
    return publicKey;
  }

  @Nonnull
  public String getKeyId() {
    return KEY_ID;
  }

  @Nonnull
  public String getAlgorithm() {
    return ALGORITHM.getValue();
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
