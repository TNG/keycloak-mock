package com.tngtech.keycloakmock.impl;

import com.tngtech.keycloakmock.api.TokenConfig;
import io.jsonwebtoken.JwtBuilder;
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
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class TokenGenerator {
  private static final String KEY_ID = "keyId";
  private static final SignatureAlgorithm ALGORITHM = SignatureAlgorithm.RS256;
  private static final String KEY = "rsa";

  @Nonnull private final Key privateKey;
  @Nonnull private final PublicKey publicKey;
  @Nonnull private final UrlConfiguration urlConfiguration;

  public TokenGenerator(@Nonnull UrlConfiguration urlConfiguration) {
    this.urlConfiguration = Objects.requireNonNull(urlConfiguration);
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
      @Nonnull final TokenConfig tokenConfig,
      @Nullable final String requestUrl,
      @Nullable final String requestRealm) {
    JwtBuilder builder =
        Jwts.builder()
            .setHeaderParam("kid", KEY_ID)
            // since the specification allows for more than one audience, but JJWT only accepts
            // one (see https://github.com/jwtk/jjwt/issues/77), use a workaround here
            .claim("aud", tokenConfig.getAudience())
            .setIssuedAt(new Date(tokenConfig.getIssuedAt().toEpochMilli()))
            .claim("auth_time", tokenConfig.getAuthenticationTime().getEpochSecond())
            .setExpiration(new Date(tokenConfig.getExpiration().toEpochMilli()))
            .setIssuer(urlConfiguration.getIssuer(requestUrl, requestRealm))
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
