package com.tngtech.keycloakmock.impl.handler;

import static com.tngtech.keycloakmock.test.KeyHelper.loadFromResource;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.mockito.Mockito.verify;

import java.security.PublicKey;
import org.junit.jupiter.api.Test;

class JwksRouteTest extends HandlerTestBase {

  @Test
  void rsaKeyIsCorrectlyExported() {
    PublicKey key = loadFromResource("/keystore.jks", "rsa");
    JwksRoute jwksRoute = new JwksRoute("key321", key);

    jwksRoute.handle(routingContext);

    verify(serverResponse).end(captor.capture());
    String jwksResponse = captor.getValue();

    assertThatJson(jwksResponse)
        .isObject()
        .containsOnlyKeys("keys")
        .extractingByKey("keys")
        .asList()
        .hasSize(1)
        .element(0)
        .hasFieldOrPropertyWithValue("kid", "key321")
        .hasFieldOrPropertyWithValue("kty", "RSA")
        .hasFieldOrPropertyWithValue("use", "sig")
        .hasFieldOrPropertyWithValue(
            "n",
            "rNp_ieKO7C0Cf2J5lrP6IZcGnWkWbp0kwDFE7prHYJX1wEV5OHGsok0WYNjuPuApGeuBbG-AkpJ3eECb8IaZcdtMzxw-L-Cyh_4Kw6iG_ssfRRhuvRSkIgmdxOJGspC2jur8047gmHUQFIsw9fszO20SGHflR723EcurfOR2RHoFHqcZLtDESDsXNjMDgiihXn04-v5O-UPFsSnBzXCRFkMZq-j5aOeAC3ub8ERC0edq_fCGK1usyaL_W1ZjxxBs70PgW5BMzvK8AItK_3gbZMK-yOb-AYmY4nJEGQjWv64Qh-HP35UgqWzCpqkpK6X4ejG_k3Jy5xFrjGLg3f118w")
        .hasFieldOrPropertyWithValue("e", "AQAB");
  }

  @Test
  void ecKeyIsCorrectlyExported() {
    PublicKey key = loadFromResource("/ec521.jks", "key");
    JwksRoute jwksRoute = new JwksRoute("key123", key);

    jwksRoute.handle(routingContext);

    verify(serverResponse).end(captor.capture());
    String jwksResponse = captor.getValue();

    assertThatJson(jwksResponse)
        .isObject()
        .containsOnlyKeys("keys")
        .extractingByKey("keys")
        .asList()
        .hasSize(1)
        .element(0)
        .hasFieldOrPropertyWithValue("kid", "key123")
        .hasFieldOrPropertyWithValue("kty", "EC")
        .hasFieldOrPropertyWithValue("use", "sig")
        .hasFieldOrPropertyWithValue("crv", "P-521")
        .hasFieldOrPropertyWithValue(
            "x",
            "AdpgZNUnFA6dFJ75CnqZNIWTV7K1RywkYChRRskEOXVv-mF2VGGu1aTEmuMqXMqAUhMkarwm8607UR4P2JIxHsZc")
        .hasFieldOrPropertyWithValue(
            "y",
            "AeS-w71tsY5dxsXT6QywvlQ6Gf-ePG8y6-53RSJN5hAvoIigA5IlhqCc2seSX3ixAZYqvYQBnUgmqbAE6r6HcpK8");
  }
}
