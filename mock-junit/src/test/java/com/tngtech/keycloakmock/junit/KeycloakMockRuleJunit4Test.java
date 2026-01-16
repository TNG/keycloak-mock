package com.tngtech.keycloakmock.junit;

import static com.tngtech.keycloakmock.api.ServerConfig.aServerConfig;
import static org.assertj.core.api.Assertions.assertThat;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpResponseExpectation;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxTestContext;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class KeycloakMockRuleJunit4Test {

  @Rule
  public KeycloakMockRule keycloakMockRule =
      new KeycloakMockRule(aServerConfig().withRandomPort().build());

  private Vertx vertx;
  private VertxTestContext testContext;
  private int port;

  @Before
  public void setup() {
    vertx = Vertx.vertx();
    testContext = new VertxTestContext();
    port = keycloakMockRule.getActualPort();
  }

  @After
  public void shutdown() throws Throwable {
    assertThat(testContext.awaitCompletion(5, TimeUnit.SECONDS)).isTrue();
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
    vertx.close();
  }

  @Test
  public void mock_is_running() {
    WebClient.create(vertx)
        .get("/auth/realms/master/protocol/openid-connect/certs")
        .port(port)
        .send()
        .expecting(HttpResponseExpectation.SC_OK.and(HttpResponseExpectation.JSON))
        .onComplete(testContext.succeedingThenComplete());
  }
}
