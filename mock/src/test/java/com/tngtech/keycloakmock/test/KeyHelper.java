package com.tngtech.keycloakmock.test;

import java.security.KeyStore;
import java.security.PublicKey;
import javax.annotation.Nonnull;

public final class KeyHelper {
  private KeyHelper() {}

  public static PublicKey loadFromResource(
      @Nonnull final String resource, @Nonnull final String alias) throws Exception {
    KeyStore keyStore = KeyStore.getInstance("JKS");
    keyStore.load(KeyHelper.class.getResourceAsStream(resource), null);
    return keyStore.getCertificate(alias).getPublicKey();
  }

  public static PublicKey loadValidKey() throws Exception {
    return loadFromResource("/keystore.jks", "rsa");
  }
}
