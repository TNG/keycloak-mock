package com.tngtech.keycloakmock.junit5;

import static com.tngtech.keycloakmock.api.ServerConfig.aServerConfig;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpResponseExpectation;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class KeycloakMockExtensionJunit5Test {
  private KeycloakMockExtension keyCloakMockExtension;

  @AfterEach
  void stopMock() {
    if (keyCloakMockExtension != null) {
      keyCloakMockExtension.afterAll(null);
    }
  }

  @Test
  void mock_is_running(Vertx vertx, VertxTestContext testContext) {
    keyCloakMockExtension = new KeycloakMockExtension(aServerConfig().withRandomPort().build());
    keyCloakMockExtension.beforeAll(null);

    WebClient.create(vertx)
        .get("/auth/realms/master/protocol/openid-connect/certs")
        .port(keyCloakMockExtension.getActualPort())
        .send()
        .expecting(HttpResponseExpectation.SC_OK.and(HttpResponseExpectation.JSON))
        .onComplete(testContext.succeedingThenComplete());
  }

  @Test
  void https_is_working(Vertx vertx, VertxTestContext testContext) {
    keyCloakMockExtension = new KeycloakMockExtension(aServerConfig().withTls(true).build());
    keyCloakMockExtension.beforeAll(null);

    WebClient.create(vertx, new WebClientOptions().setSsl(true).setTrustAll(true))
        .get("/auth/realms/master/protocol/openid-connect/certs")
        .port(keyCloakMockExtension.getActualPort())
        .send()
        .expecting(HttpResponseExpectation.SC_OK.and(HttpResponseExpectation.JSON))
        .onComplete(testContext.succeedingThenComplete());
  }
}
