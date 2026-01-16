package com.tngtech.keycloakmock.examplebackend;

import static com.tngtech.keycloakmock.api.ServerConfig.aServerConfig;
import static com.tngtech.keycloakmock.api.TokenConfig.aTokenConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.keycloakmock.junit.KeycloakMockRule;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpResponseExpectation;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxTestContext;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(
    classes = ExampleBackendApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AuthenticationJUnit4Test {

  @ClassRule
  public static KeycloakMockRule mock =
      new KeycloakMockRule(aServerConfig().withDefaultRealm("realm").build());

  @LocalServerPort private int port;

  private Vertx vertx;
  private VertxTestContext testContext;

  @Before
  public void setup() {
    vertx = Vertx.vertx();
    testContext = new VertxTestContext();
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
  public void no_authentication_fails() {
    WebClient.create(vertx)
        .get("/api/hello")
        .port(port)
        .send()
        .expecting(HttpResponseExpectation.SC_UNAUTHORIZED)
        .onComplete(testContext.succeedingThenComplete());
  }

  @Test
  public void authentication_works() {
    WebClient.create(vertx)
        .get("/api/hello")
        .port(port)
        .bearerTokenAuthentication(
            mock.getAccessToken(aTokenConfig().withSubject("Awesome").build()))
        .send()
        .expecting(HttpResponseExpectation.SC_OK)
        .map(HttpResponse::bodyAsString)
        .expecting("Hello Awesome"::equals)
        .onComplete(testContext.succeedingThenComplete());
  }

  @Test
  public void authentication_without_role_fails() {
    WebClient.create(vertx)
        .get("/api/vip")
        .port(port)
        .bearerTokenAuthentication(mock.getAccessToken(aTokenConfig().build()))
        .send()
        .expecting(HttpResponseExpectation.SC_FORBIDDEN)
        .onComplete(testContext.succeedingThenComplete());
  }

  @Test
  public void authentication_with_realm_role_works() {
    WebClient.create(vertx)
        .get("/api/vip")
        .port(port)
        .bearerTokenAuthentication(mock.getAccessToken(aTokenConfig().withRealmRole("vip").build()))
        .send()
        .expecting(HttpResponseExpectation.SC_OK)
        .map(HttpResponse::bodyAsString)
        .expecting("you may feel very special here"::equals)
        .onComplete(testContext.succeedingThenComplete());
  }

  @Test
  public void authentication_with_resource_role_works() {
    WebClient.create(vertx)
        .get("/api/vip")
        .port(port)
        .bearerTokenAuthentication(
            mock.getAccessToken(aTokenConfig().withResourceRole("server", "vip").build()))
        .send()
        .expecting(HttpResponseExpectation.SC_OK)
        .map(HttpResponse::bodyAsString)
        .expecting("you may feel very special here"::equals)
        .onComplete(testContext.succeedingThenComplete());
  }
}
