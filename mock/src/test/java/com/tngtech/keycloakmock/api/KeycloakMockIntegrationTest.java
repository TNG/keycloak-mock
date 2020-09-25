package com.tngtech.keycloakmock.api;

import static org.assertj.core.api.Assertions.assertThat;

import io.fusionauth.jwks.JSONWebKeySetHelper;
import io.fusionauth.jwks.domain.JSONWebKey;
import io.fusionauth.jwt.Verifier;
import io.fusionauth.jwt.domain.JWT;
import io.fusionauth.jwt.ec.ECVerifier;
import io.fusionauth.jwt.rsa.RSAVerifier;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class KeycloakMockIntegrationTest {

  @Test
  void generated_token_is_valid() {
    KeycloakMock keycloakMock = new KeycloakMock();
    keycloakMock.start();
    String accessToken = keycloakMock.getAccessToken(TokenConfig.aTokenConfig().build());

    List<JSONWebKey> jsonWebKeys =
        JSONWebKeySetHelper.retrieveKeysFromWellKnownConfiguration(
            "http://localhost:8000/auth/realms/master/.well-known/openid-configuration");
    Map<String, Verifier> verifierMap =
        jsonWebKeys.stream()
            .collect(
                Collectors.toMap(
                    k -> k.kid,
                    k -> {
                      PublicKey key = JSONWebKey.parse(k);
                      if (key instanceof RSAPublicKey) {
                        return RSAVerifier.newVerifier((RSAPublicKey) key);
                      } else if (key instanceof ECPublicKey) {
                        return ECVerifier.newVerifier((ECPublicKey) key);
                      } else {
                        return null;
                      }
                    }));

    JWT result = JWT.getDecoder().decode(accessToken, verifierMap);

    assertThat(result.isExpired()).isFalse();

    keycloakMock.stop();
  }
}
