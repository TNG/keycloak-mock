package com.tngtech.keycloakmock.impl.handler;

import static com.tngtech.keycloakmock.impl.handler.RequestUrlConfigurationHandler.CTX_REQUEST_CONFIGURATION;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import com.tngtech.keycloakmock.api.TokenConfig;
import com.tngtech.keycloakmock.impl.TokenGenerator;
import com.tngtech.keycloakmock.impl.UrlConfiguration;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.impl.jose.JWT;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TokenRouteTest extends HandlerTestBase {
  @Mock private UrlConfiguration urlConfiguration;
  private static final String ISSUER = "issuer";

  @Test
  void tokenSuccesful() throws Exception {
    doReturn(serverResponse).when(serverResponse).setStatusCode(ArgumentMatchers.eq(200));
    doReturn(urlConfiguration).when(routingContext).get(CTX_REQUEST_CONFIGURATION);
    doReturn(new URI(ISSUER)).when(urlConfiguration).getIssuer();
    //
    final TokenGenerator tokenGenerator = new TokenGenerator();
    TokenConfig accessTokenConfig =
        TokenConfig.aTokenConfig()
            .withSubject(RandomStringUtils.randomAlphabetic(10))
            .withScope(RandomStringUtils.randomAlphabetic(15))
            .withEmail(RandomStringUtils.randomAlphabetic(10) + "@mail.com")
            .withRealmRole("realm-role")
            .withAudience(RandomStringUtils.randomAlphabetic(15))
            .withAuthorizedParty(RandomStringUtils.randomAlphabetic(20))
            .withPreferredUsername(RandomStringUtils.randomAlphabetic(30))
            .withFamilyName(RandomStringUtils.randomAlphabetic(25))
            .withGivenName(RandomStringUtils.randomAlphabetic(15))
            .withResourceRole("resource", "role")
            .withClaim("claim1", false)
            .withExpiration(Instant.now().plusSeconds(30))
            .build();
    TokenConfig refreshTokenConfig = TokenConfig.aTokenConfig().build();
    TokenConfig idTokenConfig = TokenConfig.aTokenConfig().withEmail("email@email.com").build();
    int expiresIn = RandomUtils.nextInt(1000, 60000);
    TokenRoute tokenRoute =
        new TokenRoute()
            .withOkResponse(accessTokenConfig, idTokenConfig, refreshTokenConfig, expiresIn);

    tokenRoute.handle(routingContext);
    verify(serverResponse).end(captor.capture());
    assertThatJson(captor.getValue())
        .isObject()
        .containsKeys("access_token", "refresh_token", "id_token", "token_type", "expires_in");
    JsonObject responseBody = new JsonObject(captor.getValue());
    assertEquals("Bearer", responseBody.getString("token_type"));
    assertEquals(expiresIn, responseBody.getInteger("expires_in"));
    assertEquals(
        tokenGenerator.getToken(accessTokenConfig, urlConfiguration),
        responseBody.getString("access_token"));
    assertTokenData(JWT.parse(responseBody.getString("access_token")).encode(), accessTokenConfig);
    assertEquals(
        tokenGenerator.getToken(refreshTokenConfig, urlConfiguration),
        responseBody.getString("refresh_token"));
    assertEquals(
        tokenGenerator.getToken(idTokenConfig, urlConfiguration),
        responseBody.getString("id_token"));
  }

  /**
   * The <b>assertTokenData</b> method returns {@link void}
   *
   * @param encode
   * @param tokenConfig
   */
  private void assertTokenData(String encode, TokenConfig tokenConfig) {
    assertThatJson(encode)
        .and(
            a -> a.node("header.kid").isEqualTo("keyId"),
            a -> a.node("header.alg").isEqualTo("RS256"),
            a ->
                a.node("payload.aud")
                    .isArray()
                    .hasSize(1)
                    .element(0)
                    .isEqualTo(tokenConfig.getAudience().iterator().next()),
            a -> a.node("payload.iat").isNumber().isNotNegative(),
            a -> a.node("payload.auth_time").isNumber().isNotNegative(),
            a ->
                a.node("payload.exp")
                    .isNumber()
                    .isEqualByComparingTo(
                        BigDecimal.valueOf(tokenConfig.getExpiration().getEpochSecond())),
            a -> a.node("payload.scope").isString().isEqualTo(tokenConfig.getScope()),
            a -> a.node("payload.azp").isString().isEqualTo(tokenConfig.getAuthorizedParty()),
            a -> a.node("payload.sub").isString().isEqualTo(tokenConfig.getSubject()),
            a ->
                a.node("payload.preferred_username")
                    .isString()
                    .isEqualTo(tokenConfig.getPreferredUsername()),
            a -> a.node("payload.family_name").isString().isEqualTo(tokenConfig.getFamilyName()),
            a -> a.node("payload.given_name").isString().isEqualTo(tokenConfig.getGivenName()),
            a -> a.node("payload.email").isString().isEqualTo(tokenConfig.getEmail()),
            a ->
                a.node("payload.realm_access.roles")
                    .isArray()
                    .hasSize(1)
                    .element(0)
                    .isEqualTo("realm-role"),
            a ->
                a.node("payload.resource_access.resource.roles")
                    .isArray()
                    .hasSize(1)
                    .element(0)
                    .isEqualTo("role"),
            a -> a.node("payload.claim1").isBoolean().isFalse(),
            a -> a.node("signatureBase").isString().isNotEmpty(),
            a -> a.node("signature").isString().isNotEmpty());
  }

  @Test
  void tokenError() throws Exception {
    doReturn(serverResponse).when(serverResponse).setStatusCode(ArgumentMatchers.eq(404));

    //
    JsonObject jsonErrorObject = new JsonObject();
    jsonErrorObject.put("error", "Error detail message");
    TokenRoute tokenRoute = new TokenRoute().withErrorResponse(404, jsonErrorObject.encode());
    tokenRoute.handle(routingContext);
    verify(serverResponse).end(captor.capture());
    assertThatJson(captor.getValue())
        .isObject()
        .containsOnlyKeys("error")
        .hasFieldOrPropertyWithValue("error", "Error detail message");

    verify(serverResponse).setStatusCode(404);
  }
}
