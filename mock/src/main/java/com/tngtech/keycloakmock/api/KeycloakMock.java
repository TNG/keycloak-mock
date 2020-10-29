package com.tngtech.keycloakmock.api;

import com.tngtech.keycloakmock.impl.TokenGenerator;
import com.tngtech.keycloakmock.impl.UrlConfiguration;
import com.tngtech.keycloakmock.impl.handler.JwksRoute;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A mock of a keycloak instance capable of producing access tokens.
 *
 * <p>This can be used in component tests of REST endpoints that are secured via Keycloak. The mock
 * provides a JWKS endpoint so the signature of the access token can be verified.
 *
 * <p>Typically, you should not need to use this class directly. Consider using {@code
 * com.tngtech.keycloakmock.junit.KeycloakMock} from module mock-junit instead.
 */
public class KeycloakMock {
  private static final Logger LOG = LoggerFactory.getLogger(KeycloakMock.class);

  private static final String OPEN_ID_CONFIG_TEMPLATE =
      "{\n"
          + "  \"issuer\": \"%1$s\",\n"
          + "  \"authorization_endpoint\": \"%2$s/authenticate\",\n"
          + "  \"token_endpoint\": \"%1$s/protocol/openid-connect/token\",\n"
          + "  \"jwks_uri\": \"%1$s/protocol/openid-connect/certs\",\n"
          + "  \"response_types_supported\": [\n"
          + "    \"code\",\n"
          + "    \"code id_token\",\n"
          + "    \"id_token\",\n"
          + "    \"token id_token\"\n"
          + "  ],\n"
          + "  \"subject_types_supported\": [\n"
          + "    \"public\"\n"
          + "  ],\n"
          + "  \"id_token_signing_alg_values_supported\": [\n"
          + "    \"RS256\",\n"
          + "    \"ES256\",\n"
          + "    \"HS256\"\n"
          + "  ],"
          + "  \"end_session_endpoint\": \"%2$s/logout\"\n"
          + "}";
  @Nonnull protected final Vertx vertx = Vertx.vertx();
  @Nonnull protected final TokenGenerator tokenGenerator;
  @Nonnull private final UrlConfiguration urlConfiguration;
  @Nonnull private final JwksRoute jwksRoute;
  @Nullable private HttpServer server;

  /**
   * Create a mock instance for realm "master".
   *
   * <p>The JWKS endpoint is served via HTTP on port 8000.
   *
   * @throws IllegalStateException when the built-in keystore could not be read
   * @see KeycloakMock#KeycloakMock(ServerConfig)
   */
  public KeycloakMock() {
    this(ServerConfig.aServerConfig().build());
  }

  /**
   * Create a mock instance for a given realm.
   *
   * <p>Depending on the tls parameter, the JWKS endpoint is served via HTTP or HTTPS.
   *
   * @param serverConfig the server configuration to use
   * @throws IllegalStateException when the built-in keystore could not be read
   * @see KeycloakMock#KeycloakMock()
   */
  public KeycloakMock(@Nonnull final ServerConfig serverConfig) {
    this.urlConfiguration = new UrlConfiguration(serverConfig);
    this.tokenGenerator = new TokenGenerator(urlConfiguration);
    this.jwksRoute =
        new JwksRoute(
            tokenGenerator.getKeyId(),
            tokenGenerator.getAlgorithm(),
            tokenGenerator.getPublicKey());
  }

  /**
   * Get a signed access token for the given parameters.
   *
   * @param tokenConfig the configuration of the token to generate
   * @return an access token in compact JWT form
   * @see TokenConfig.Builder
   */
  @Nonnull
  public String getAccessToken(@Nonnull final TokenConfig tokenConfig) {
    return tokenGenerator.getToken(tokenConfig, null, null);
  }

  /**
   * Start the server (blocking).
   *
   * @throws MockServerException when the server could not be started properly
   * @throws IllegalStateException when the built-in keystore could not be read for TLS mode
   */
  public void start() {
    if (server != null) {
      LOG.warn("Start request ignored as server is already running");
      return;
    }
    HttpServerOptions options = new HttpServerOptions().setPort(urlConfiguration.getPort());
    if (urlConfiguration.getProtocol().isTls()) {
      options
          .setSsl(true)
          .setKeyStoreOptions(new JksOptions().setValue(getKeystore()).setPassword(""));
    }
    Router router = configureRouter();
    ResultHandler<HttpServer> startHandler = new ResultHandler<>();

    server =
        vertx
            .createHttpServer(options)
            .requestHandler(router)
            .exceptionHandler(t -> LOG.error("Exception while processing request", t))
            .listen(startHandler);
    startHandler.await();
  }

  @Nonnull
  protected Router configureRouter() {
    Router router = Router.router(vertx);
    router.get("/auth/realms/:realm/protocol/openid-connect/certs").handler(jwksRoute);
    router
        .get("/auth/realms/:realm/.well-known/openid-configuration")
        .handler(this::getOpenIdConfig);
    return router;
  }

  /**
   * Stop the server (blocking).
   *
   * @throws MockServerException when the server could not be stopped properly
   */
  public void stop() {
    if (server != null) {
      ResultHandler<Void> stopHandler = new ResultHandler<>();
      server.close(stopHandler);
      stopHandler.await();
      server = null;
    }
  }

  @Nonnull
  private Buffer getKeystore() {
    try {
      InputStream inputStream = this.getClass().getResourceAsStream("/keystore.jks");
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      byte[] buf = new byte[8192];
      int n;
      while ((n = inputStream.read(buf)) > 0) {
        outputStream.write(buf, 0, n);
      }
      return Buffer.buffer(outputStream.toByteArray());
    } catch (IOException e) {
      throw new IllegalStateException("Error while loading keystore for TLS key configuration", e);
    }
  }

  private void getOpenIdConfig(@Nonnull final RoutingContext routingContext) {
    final String requestRealm = routingContext.pathParam("realm");
    String requestHostname = routingContext.request().getHeader("Host");
    routingContext
        .response()
        .putHeader("content-type", "application/json")
        .end(
            String.format(
                OPEN_ID_CONFIG_TEMPLATE,
                urlConfiguration.getIssuer(requestHostname, requestRealm),
                urlConfiguration.getBaseUrl(requestHostname)));
  }

  private static class ResultHandler<E> implements Handler<AsyncResult<E>> {

    @Nonnull private final CompletableFuture<Void> future = new CompletableFuture<>();

    @Override
    public void handle(@Nonnull final AsyncResult<E> result) {
      if (result.succeeded()) {
        future.complete(null);
      } else {
        future.completeExceptionally(result.cause());
      }
    }

    void await() {
      try {
        future.get();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new MockServerException("Interrupted while starting/stopping mock server", e);
      } catch (ExecutionException e) {
        throw new MockServerException(
            "Error while starting/stopping mock server: " + e.getMessage(), e);
      }
    }
  }
}
