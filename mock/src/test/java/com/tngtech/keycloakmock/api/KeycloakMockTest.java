package com.tngtech.keycloakmock.api;

import static com.tngtech.keycloakmock.api.ServerConfig.aServerConfig;
import static com.tngtech.keycloakmock.test.KeyHelper.loadValidKey;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import java.nio.charset.StandardCharsets;
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
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

class KeycloakMockTest {

  @Test
  void generated_token_uses_correct_issuer() throws Exception {
    JwtParser jwtParser = Jwts.parserBuilder().setSigningKey(loadValidKey()).build();
    KeycloakMock keycloakMock =
        new KeycloakMock(
            aServerConfig()
                .withPort(123)
                .withDefaultRealm("realm123")
                .withDefaultHostname("somehost")
                .build());

    String token = keycloakMock.getAccessToken(TokenConfig.aTokenConfig().build());

    Jws<Claims> jwt = jwtParser.parseClaimsJws(token);

    assertThat(jwt.getBody().getIssuer()).isEqualTo("http://somehost:123/auth/realms/realm123");
  }

  @Test
  void contains_client_scopes_during_server_configuration() throws Exception {
    Set<String> scopes = prepareScopes();
    KeycloakMock keycloakMock =
        new KeycloakMock(
            aServerConfig()
                .withPort(9005)
                .withDefaultRealm("test-demo")
                .withClientScopes(scopes)
                .build());
    keycloakMock.start();

    String token = getAuthorization();

    Set<String> scope = extractScopeFromToken(token);

    assertEquals(3, scope.size());
    assertTrue(
        scope.contains("openid") && scope.contains("TestScope1") && scope.contains("TestScope2"));

    aServerConfig().resetClientScopes().build();

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
    String token = oAuthResponse.getAccessToken();

    return token;
  }

  private Set<String> extractScopeFromToken(String token) {
    String[] chunks = token.split("\\.");

    Base64.Decoder decoder = Base64.getUrlDecoder();

    String payload = new String(decoder.decode(chunks[1]));

    JSONObject json = new JSONObject(payload);

    String scope = json.getString("scope");
    Set<String> items = new HashSet<String>(Arrays.asList(scope.split(" ")));

    return items;
  }

  private static String getBase64Encoded(String id, String password) {
    return Base64.getEncoder()
        .encodeToString((id + ":" + password).getBytes(StandardCharsets.UTF_8));
  }
}
