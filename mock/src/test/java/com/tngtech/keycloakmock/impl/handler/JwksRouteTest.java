package com.tngtech.keycloakmock.impl.handler;

import static com.tngtech.keycloakmock.test.KeyHelper.loadFromResource;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.mockito.Mockito.verify;

import io.jsonwebtoken.SignatureAlgorithm;
import java.security.PublicKey;
import org.junit.jupiter.api.Test;

class JwksRouteTest extends HandlerTestBase {

  @Test
  void rsaKeyIsCorrectlyExported() throws Exception {
    PublicKey key = loadFromResource("/keystore.jks", "rsa");
    JwksRoute jwksRoute = new JwksRoute("key321", SignatureAlgorithm.RS256, key);

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
        .hasFieldOrPropertyWithValue("alg", "RS256")
        .hasFieldOrPropertyWithValue("use", "sig")
        .hasFieldOrPropertyWithValue(
            "n",
            "AKzaf4nijuwtAn9ieZaz-iGXBp1pFm6dJMAxRO6ax2CV9cBFeThxrKJNFmDY7j7gKRnrgWxvgJKSd3hAm_CGmXHbTM8cPi_gsof-CsOohv7LH0UYbr0UpCIJncTiRrKQto7q_NOO4Jh1EBSLMPX7MzttEhh35Ue9txHLq3zkdkR6BR6nGS7QxEg7FzYzA4IooV59OPr-TvlDxbEpwc1wkRZDGavo-WjngAt7m_BEQtHnav3whitbrMmi_1tWY8cQbO9D4FuQTM7yvACLSv94G2TCvsjm_gGJmOJyRBkI1r-uEIfhz9-VIKlswqapKSul-Hoxv5NycucRa4xi4N39dfM")
        .hasFieldOrPropertyWithValue("e", "AQAB");
  }

  @Test
  void ecKeyIsCorrectlyExported() throws Exception {
    PublicKey key = loadFromResource("/ec521.jks", "key");
    JwksRoute jwksRoute = new JwksRoute("key123", SignatureAlgorithm.ES512, key);

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
        .hasFieldOrPropertyWithValue("alg", "ES512")
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
