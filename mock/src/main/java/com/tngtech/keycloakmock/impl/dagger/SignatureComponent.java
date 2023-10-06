package com.tngtech.keycloakmock.impl.dagger;

import com.tngtech.keycloakmock.impl.TokenGenerator;
import dagger.Component;
import java.security.KeyStore;
import java.security.PublicKey;
import javax.inject.Named;
import javax.inject.Singleton;

@Component(modules = KeyModule.class)
@Singleton
public interface SignatureComponent {
  // Note that while this is currently the same keystore used for storing the signing key-pair,
  // this is just a coincidence. It is provided here only to allow setting up a self-signed TLS
  // endpoint with a separate key-pair.
  KeyStore keyStore();

  PublicKey publicKey();

  @Named("keyId")
  String keyId();

  TokenGenerator tokenGenerator();
}
