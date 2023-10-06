package com.tngtech.keycloakmock.test;

import java.security.KeyStore;
import java.security.PublicKey;
import javax.annotation.Nonnull;

public final class KeyHelper {
  private KeyHelper() {}

  public static PublicKey loadFromResource(
      @Nonnull final String resource, @Nonnull final String alias) {
    try {
      KeyStore keyStore = KeyStore.getInstance("JKS");
      keyStore.load(KeyHelper.class.getResourceAsStream(resource), null);
      return keyStore.getCertificate(alias).getPublicKey();
    } catch (Exception e) {
      throw new IllegalStateException("Unable to access key " + alias + " in " + resource);
    }
  }

  public static PublicKey loadValidKey() {
    return loadFromResource("/keystore.jks", "rsa");
  }
}
