package com.tngtech.keycloakmock.api.handler;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import java.security.KeyStore;
import java.security.PublicKey;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JwksRouteTest {

  @Mock RoutingContext routingContext;

  @Mock HttpServerResponse serverResponse;

  @Captor ArgumentCaptor<String> captor;

  @BeforeEach
  void setup() {
    doReturn(serverResponse).when(routingContext).response();
    doReturn(serverResponse).when(serverResponse).putHeader(anyString(), anyString());
  }

  @Test
  void rsaKeyIsCorrectlyExported() throws Exception {
    PublicKey key = loadFromResource("/keystore.jks", "rsa");
    JwksRoute jwksRoute = new JwksRoute("key321", "RS256", key);

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
            "AKzaf4nijuwtAn9ieZaz-iGXBp1pFm6dJMAxRO6ax2CV9cBFeThxrKJNFmDY7j7gKRnrgWxvgJKSd3hAm_CGmXHbTM8cPi_gsof-CsOohv7LH0UYbr0UpCIJncTiRrKQto7q_NOO4Jh1EBSLMPX7MzttEhh35Ue9txHLq3zkdkR6BR6nGS7QxEg7FzYzA4IooV59OPr-TvlDxbEpwc1wkRZDGavo-WjngAt7m_BEQtHnav3whitbrMmi_1tWY8cQbO9D4FuQTM7yvACLSv94G2TCvsjm_gGJmOJyRBkI1r-uEIfhz9-VIKlswqapKSul-Hoxv5NycucRa4xi4N39dfM=")
        .hasFieldOrPropertyWithValue("e", "AQAB");
  }

  @Test
  void ecKeyIsCorrectlyExported() throws Exception {
    PublicKey key = loadFromResource("/ec521.jks", "key");
    JwksRoute jwksRoute = new JwksRoute("key123", "ES512", key);

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

  private PublicKey loadFromResource(@Nonnull final String resource, @Nonnull final String alias)
      throws Exception {
    KeyStore keyStore = KeyStore.getInstance("JKS");
    keyStore.load(getClass().getResourceAsStream(resource), null);
    return keyStore.getCertificate(alias).getPublicKey();
  }
}
