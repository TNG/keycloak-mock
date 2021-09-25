package com.tngtech.keycloakmock.impl.dagger;

import dagger.Module;
import dagger.Provides;
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
import java.util.Objects;
import javax.inject.Named;
import javax.inject.Singleton;

@Module
class KeyModule {
  private static final String KEY = "rsa";

  @Provides
  @Named("keyId")
  @Singleton
  String provideKeyId() {
    return "keyId";
  }

  @Provides
  @Singleton
  SignatureAlgorithm provideSignatureAlgorithm() {
    return SignatureAlgorithm.RS256;
  }

  @Provides
  @Singleton
  KeyStore provideKeystore() {
    try {
      KeyStore keyStore = KeyStore.getInstance("JKS");
      try (InputStream keystoreStream = this.getClass().getResourceAsStream("/keystore.jks")) {
        keyStore.load(keystoreStream, null);
      }
      return keyStore;
    } catch (IOException | CertificateException | NoSuchAlgorithmException | KeyStoreException e) {
      throw new IllegalStateException("Error while loading keystore for signing token", e);
    }
  }

  @Provides
  @Singleton
  PublicKey providePublicKey(KeyStore keyStore) {
    try {
      return Objects.requireNonNull(keyStore.getCertificate(KEY).getPublicKey());
    } catch (KeyStoreException e) {
      throw new IllegalStateException("Error while loading public key for verifying token", e);
    }
  }

  @Provides
  @Singleton
  Key provideSecretKey(KeyStore keyStore) {
    try {
      return Objects.requireNonNull(keyStore.getKey(KEY, new char[] {}));
    } catch (KeyStoreException | UnrecoverableKeyException | NoSuchAlgorithmException e) {
      throw new IllegalStateException("Error while loading private key for signing token", e);
    }
  }
}
