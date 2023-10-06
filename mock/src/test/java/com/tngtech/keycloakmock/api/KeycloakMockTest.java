package com.tngtech.keycloakmock.api;

import static com.tngtech.keycloakmock.api.ServerConfig.aServerConfig;
import static com.tngtech.keycloakmock.test.KeyHelper.loadValidKey;
import static org.assertj.core.api.Assertions.assertThat;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthJSONAccessTokenResponse;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.junit.jupiter.api.Test;

class KeycloakMockTest {
  private final PublicKey publicKey = loadValidKey();

  @Test
  void generated_token_uses_correct_issuer() {
    JwtParser jwtParser = Jwts.parser().verifyWith(publicKey).build();
    KeycloakMock keycloakMock =
        new KeycloakMock(
            aServerConfig()
                .withPort(123)
                .withDefaultRealm("realm123")
                .withDefaultHostname("somehost")
                .build());

    String token = keycloakMock.getAccessToken(TokenConfig.aTokenConfig().build());

    Jws<Claims> jwt = jwtParser.parseSignedClaims(token);

    assertThat(jwt.getPayload().getIssuer()).isEqualTo("http://somehost:123/auth/realms/realm123");
  }

  @Test
  void contains_client_scopes_during_server_configuration() throws Exception {
    Set<String> scopes = prepareScopes();
    KeycloakMock keycloakMock =
        new KeycloakMock(
            aServerConfig()
                .withPort(9005)
                .withDefaultRealm("test-demo")
                .withDefaultScopes(scopes)
                .build());
    keycloakMock.start();

    String token = getAuthorization();

    Set<String> scope = extractScopeFromToken(token);

    assertThat(scope).hasSize(3).containsExactlyInAnyOrder("openid", "TestScope1", "TestScope2");

    keycloakMock.stop();
  }

  @Test
  void contains_default_client_scope_during_server_configuration() throws Exception {
    KeycloakMock keycloakMock =
        new KeycloakMock(aServerConfig().withPort(9005).withDefaultRealm("test-demo").build());
    keycloakMock.start();

    String token = getAuthorization();

    Set<String> scope = extractScopeFromToken(token);

    assertThat(scope).hasSize(1).containsOnly("openid");

    keycloakMock.stop();
  }

  private Set<String> prepareScopes() {
    Set<String> scopes = new HashSet<>();
    scopes.add("TestScope1");
    scopes.add("TestScope2");

    return scopes;
  }

  private String getAuthorization() throws OAuthSystemException, OAuthProblemException {
    String clientId = "test-demo";
    String clientSecret = "OZtOv3TlXvEhhKf705Z53J8QL8YPY9UJ";
    String tokenURL = "http://127.0.0.1:9005/auth/realms/test-demo/protocol/openid-connect/token";

    String encodedValue = getBase64Encoded(clientId, clientSecret);

    OAuthClient client = new OAuthClient(new URLConnectionClient());

    OAuthClientRequest request =
        OAuthClientRequest.tokenLocation(tokenURL)
            .setGrantType(GrantType.CLIENT_CREDENTIALS)
            .buildBodyMessage();
    request.addHeader("Authorization", "Basic " + encodedValue);

    OAuthJSONAccessTokenResponse oAuthResponse =
        client.accessToken(request, OAuth.HttpMethod.POST, OAuthJSONAccessTokenResponse.class);

    return oAuthResponse.getAccessToken();
  }

  private Set<String> extractScopeFromToken(String token) {
    String scope =
        Jwts.parser()
            .verifyWith(publicKey)
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .get("scope", String.class);

    return new HashSet<>(Arrays.asList(scope.split(" ")));
  }

  private static String getBase64Encoded(String id, String password) {
    return Base64.getEncoder()
        .encodeToString((id + ":" + password).getBytes(StandardCharsets.UTF_8));
  }
}
