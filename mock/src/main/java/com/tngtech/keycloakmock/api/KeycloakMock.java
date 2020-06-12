package com.tngtech.keycloakmock.api;

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
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A mock of a keycloak instance capable of producing access tokens.
 *
 * <p>This can be used in component tests of REST endpoints that are secured via Keycloak. The mock
 * provides a JWKS endpoint so the signature of the access token can be verified.
 *
 * <p>Typically, you should not need to use this class directly. Consider using {@code
 * com.tngtech.keycloakmock.junit.KeycloakMock} from module mock-junit or {@code
 * com.tngtech.keycloakmock.junit5.KeycloakMock} from module mock-junit5 instead.
 */
public class KeycloakMock {
  private static final Logger LOG = LoggerFactory.getLogger(KeycloakMock.class);

  public static final int DEFAULT_PORT = 8000;
  public static final String DEFAULT_REALM = "master";
  private static final String HTTP = "http";
  private static final String HTTPS = "https";
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
  private static final String ISSUER_TEMPLATE = "%s/auth/realms/%s";
  @Nonnull private final TokenGenerator tokenGenerator;
  private final int port;
  @Nonnull private final String realm;
  private final boolean tls;
  @Nonnull private final String hostname;
  protected final Vertx vertx = Vertx.vertx();
  private HttpServer server;

  /**
   * Create a mock instance for realm "master".
   *
   * <p>The JWKS endpoint is served via HTTP on port 8000. If you need HTTPS or a different realm or
   * port use {@link KeycloakMock#KeycloakMock(int, String, boolean)} instead.
   *
   * @throws IllegalStateException when the built-in keystore could not be read
   */
  public KeycloakMock() {
    this(DEFAULT_PORT, DEFAULT_REALM);
  }

  /**
   * Create a mock instance for a given realm.
   *
   * <p>The JWKS endpoint is served via HTTP. If you need HTTPS, use {@link
   * KeycloakMock#KeycloakMock(int, String, boolean)} instead.
   *
   * @param port the port of the mock to run (e.g. 8000)
   * @param realm the realm for which to provide tokens
   * @throws IllegalStateException when the built-in keystore could not be read
   */
  public KeycloakMock(final int port, @Nonnull final String realm) {
    this(port, realm, false);
  }

  /**
   * Create a mock instance for a given realm.
   *
   * <p>Depending on the tls parameter, the JWKS endpoint is served via HTTP or HTTPS.
   *
   * @param port the port of the mock to run (e.g. 8000)
   * @param realm the realm for which to provide tokens
   * @param tls whether to use HTTPS instead of HTTP
   * @throws IllegalStateException when the built-in keystore could not be read
   */
  public KeycloakMock(final int port, @Nonnull final String realm, final boolean tls) {
    this.port = port;
    this.realm = Objects.requireNonNull(realm);
    this.tls = tls;
    this.hostname = tls ? HTTPS : HTTP + "://localhost:" + port;
    this.tokenGenerator = new TokenGenerator();
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
    return tokenGenerator.getToken(tokenConfig, getIssuer(realm));
  }

  @Nonnull
  protected String getAccessTokenForRealm(
      @Nonnull final TokenConfig tokenConfig, @Nonnull final String realm) {
    return tokenGenerator.getToken(tokenConfig, getIssuer(realm));
  }

  /**
   * Start the server (blocking).
   *
   * @throws MockServerException when the server could not be started properly
   * @throws IllegalStateException when the built-in keystore could not be read for TLS mode
   */
  public void start() {
    HttpServerOptions options = new HttpServerOptions().setPort(port);
    if (tls) {
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

  protected Router configureRouter() {
    Router router = Router.router(vertx);
    router.get("/auth/realms/:realm/protocol/openid-connect/certs").handler(this::getJwksResponse);
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
    ResultHandler<Void> stopHandler = new ResultHandler<>();
    server.close(stopHandler);
    stopHandler.await();
  }

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
      LOG.error("Error while loading keystore for TLS key configuration", e);
      throw new IllegalStateException(e);
    }
  }

  private void getJwksResponse(final RoutingContext routingContext) {
    routingContext
        .response()
        .putHeader("content-type", "application/json")
        .end(tokenGenerator.getJwksResponse());
  }

  private void getOpenIdConfig(final RoutingContext routingContext) {
    final String requestRealm = routingContext.pathParam("realm");
    routingContext
        .response()
        .putHeader("content-type", "application/json")
        .end(String.format(OPEN_ID_CONFIG_TEMPLATE, getIssuer(requestRealm), hostname));
  }

  @Nonnull
  private String getIssuer(@Nonnull final String realm) {
    return String.format(ISSUER_TEMPLATE, hostname, realm);
  }

  private static class ResultHandler<E> implements Handler<AsyncResult<E>> {

    private final CompletableFuture<Void> future = new CompletableFuture<>();

    @Override
    public void handle(AsyncResult<E> result) {
      if (result.succeeded()) {
        future.complete(null);
      } else {
        future.completeExceptionally(result.cause());
      }
    }

    void await() {
      try {
        future.get();
      } catch (InterruptedException | ExecutionException e) {
        LOG.error("Error while starting/stopping mock server", e);
        Thread.currentThread().interrupt();
        throw new MockServerException(e);
      }
    }
  }
}
