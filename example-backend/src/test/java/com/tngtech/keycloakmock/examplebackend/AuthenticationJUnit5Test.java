package com.tngtech.keycloakmock.examplebackend;

import static com.tngtech.keycloakmock.api.ServerConfig.aServerConfig;
import static com.tngtech.keycloakmock.api.TokenConfig.aTokenConfig;

import com.tngtech.keycloakmock.junit5.KeycloakMockExtension;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpResponseExpectation;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(
    classes = ExampleBackendApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(VertxExtension.class)
class AuthenticationJUnit5Test {

  @RegisterExtension
  static KeycloakMockExtension mock =
      new KeycloakMockExtension(aServerConfig().withDefaultRealm("realm").build());

  @LocalServerPort int port;

  @Test
  void no_authentication_fails(Vertx vertx, VertxTestContext testContext) {
    WebClient.create(vertx)
        .get("/api/hello")
        .port(port)
        .send()
        .expecting(HttpResponseExpectation.SC_UNAUTHORIZED)
        .onComplete(testContext.succeedingThenComplete());
  }

  @Test
  void authentication_works(Vertx vertx, VertxTestContext testContext) {
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
  void authentication_without_role_fails(Vertx vertx, VertxTestContext testContext) {
    WebClient.create(vertx)
        .get("/api/vip")
        .port(port)
        .bearerTokenAuthentication(mock.getAccessToken(aTokenConfig().build()))
        .send()
        .expecting(HttpResponseExpectation.SC_FORBIDDEN)
        .onComplete(testContext.succeedingThenComplete());
  }

  @Test
  void authentication_with_realm_role_works(Vertx vertx, VertxTestContext testContext) {
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
  void authentication_with_resource_role_works(Vertx vertx, VertxTestContext testContext) {
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
