package com.tngtech.keycloakmock.api;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.keycloakmock.test.ConfigurationResponse;
import io.fusionauth.jwks.JSONWebKeySetHelper;
import io.fusionauth.jwks.domain.JSONWebKey;
import io.fusionauth.jwt.Verifier;
import io.fusionauth.jwt.domain.JWT;
import io.fusionauth.jwt.ec.ECVerifier;
import io.fusionauth.jwt.rsa.RSAVerifier;
import io.restassured.http.ContentType;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class KeycloakMockIntegrationTest {

  private KeycloakMock keycloakMock = null;

  @BeforeEach
  void setup() {
    keycloakMock = new KeycloakMock();
    keycloakMock.start();
  }

  @AfterEach
  void tearDown() {
    if (keycloakMock != null) {
      keycloakMock.stop();
    }
  }

  @Test
  void generated_token_is_valid() {
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
  }

  @Test
  void well_known_configuration_works() {
    ConfigurationResponse response =
        given()
            .header("Host", "server")
            .when()
            .get("http://localhost:8000/auth/realms/test/.well-known/openid-configuration")
            .then()
            .assertThat()
            .statusCode(200)
            .and()
            .contentType(ContentType.JSON)
            .and()
            .extract()
            .body()
            .as(ConfigurationResponse.class);

    assertThat(response.authorization_endpoint)
        .isEqualTo("http://server/auth/realms/test/protocol/openid-connect/auth");
    assertThat(response.end_session_endpoint)
        .isEqualTo("http://server/auth/realms/test/protocol/openid-connect/logout");
    assertThat(response.id_token_signing_alg_values_supported).containsExactly("RS256");
    assertThat(response.issuer).isEqualTo("http://server/auth/realms/test");
    assertThat(response.jwks_uri)
        .isEqualTo("http://server/auth/realms/test/protocol/openid-connect/certs");
    assertThat(response.response_types_supported)
        .containsExactlyInAnyOrder("code", "code id_token", "id_token", "token id_token");
    assertThat(response.subject_types_supported).containsExactly("public");
    assertThat(response.token_endpoint)
        .isEqualTo("http://server/auth/realms/test/protocol/openid-connect/token");
  }
}
