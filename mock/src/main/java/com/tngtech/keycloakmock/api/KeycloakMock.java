package com.tngtech.keycloakmock.api;

import com.tngtech.keycloakmock.impl.TokenGenerator;
import com.tngtech.keycloakmock.impl.UrlConfiguration;
import com.tngtech.keycloakmock.impl.handler.AuthenticationRoute;
import com.tngtech.keycloakmock.impl.handler.CommonHandler;
import com.tngtech.keycloakmock.impl.handler.FailureHandler;
import com.tngtech.keycloakmock.impl.handler.IFrameRoute;
import com.tngtech.keycloakmock.impl.handler.JwksRoute;
import com.tngtech.keycloakmock.impl.handler.LoginRoute;
import com.tngtech.keycloakmock.impl.handler.LogoutRoute;
import com.tngtech.keycloakmock.impl.handler.RequestUrlConfigurationHandler;
import com.tngtech.keycloakmock.impl.handler.ResourceFileHandler;
import com.tngtech.keycloakmock.impl.handler.TokenRoute;
import com.tngtech.keycloakmock.impl.handler.WellKnownRoute;
import com.tngtech.keycloakmock.impl.helper.RedirectHelper;
import com.tngtech.keycloakmock.impl.helper.TokenHelper;
import com.tngtech.keycloakmock.impl.session.SessionRepository;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.ErrorHandler;
import io.vertx.ext.web.templ.freemarker.FreeMarkerTemplateEngine;
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
 * provides a REST API compatible with a real Keycloak to use in tests.
 *
 * <p>Typically, you should not need to use this class directly. Consider using {@code
 * com.tngtech.keycloakmock.junit.KeycloakMock} from module mock-junit instead.
 */
public class KeycloakMock {
  private static final Logger LOG = LoggerFactory.getLogger(KeycloakMock.class);

  @Nonnull private final Vertx vertx = Vertx.vertx();
  @Nonnull private final CommonHandler commonHandler = new CommonHandler();
  @Nonnull private final FailureHandler failureHandler = new FailureHandler();
  @Nonnull private final TokenGenerator tokenGenerator;
  @Nonnull private final UrlConfiguration urlConfiguration;
  @Nonnull private final RequestUrlConfigurationHandler requestUrlConfigurationHandler;
  @Nonnull private final JwksRoute jwksRoute;
  @Nonnull private final WellKnownRoute wellKnownRoute;
  @Nonnull private final LoginRoute loginRoute;
  @Nonnull private final AuthenticationRoute authenticationRoute;
  @Nonnull private final TokenRoute tokenRoute;
  @Nonnull private final LogoutRoute logoutRoute;
  @Nonnull private final IFrameRoute iframeRoute = new IFrameRoute();

  @Nonnull
  private final ResourceFileHandler thirdPartyCookies1Route =
      new ResourceFileHandler("/3p-cookies-step1.html");

  @Nonnull
  private final ResourceFileHandler thirdPartyCookies2Route =
      new ResourceFileHandler("/3p-cookies-step2.html");

  @Nonnull
  private final ResourceFileHandler keycloakJsRoute = new ResourceFileHandler("/keycloak.js");

  @Nullable private HttpServer server;

  /**
   * Create a mock instance for default realm "master".
   *
   * <p>The JWKS endpoint is served via HTTP on localhost with port 8000.
   *
   * @throws IllegalStateException when the built-in keystore could not be read
   * @see KeycloakMock#KeycloakMock(ServerConfig)
   */
  public KeycloakMock() {
    this(ServerConfig.aServerConfig().build());
  }

  /**
   * Create a mock instance for a given server configuration.
   *
   * <p>Depending on the tls parameter, the JWKS endpoint is served via HTTP or HTTPS.
   *
   * @param serverConfig the server configuration to use
   * @throws IllegalStateException when the built-in keystore could not be read
   * @see KeycloakMock#KeycloakMock()
   */
  public KeycloakMock(@Nonnull final ServerConfig serverConfig) {
    this.urlConfiguration = new UrlConfiguration(serverConfig);
    this.tokenGenerator = new TokenGenerator();
    this.requestUrlConfigurationHandler = new RequestUrlConfigurationHandler(urlConfiguration);
    this.jwksRoute =
        new JwksRoute(
            tokenGenerator.getKeyId(),
            tokenGenerator.getAlgorithm(),
            tokenGenerator.getPublicKey());
    this.wellKnownRoute = new WellKnownRoute();
    TokenHelper tokenHelper =
        new TokenHelper(tokenGenerator, serverConfig.getResourcesToMapRolesTo());
    RedirectHelper redirectHelper = new RedirectHelper(tokenHelper);
    SessionRepository sessionRepository = new SessionRepository();
    loginRoute =
        new LoginRoute(sessionRepository, redirectHelper, FreeMarkerTemplateEngine.create(vertx));
    authenticationRoute = new AuthenticationRoute(sessionRepository, redirectHelper);
    tokenRoute = new TokenRoute(sessionRepository, tokenHelper);
    logoutRoute = new LogoutRoute(sessionRepository, redirectHelper);
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
    return tokenGenerator.getToken(tokenConfig, urlConfiguration);
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
  private Router configureRouter() {
    UrlConfiguration routing = urlConfiguration.forRequestContext(null, ":realm");
    Router router = Router.router(vertx);
    router
        .route()
        .handler(requestUrlConfigurationHandler)
        .handler(commonHandler)
        .failureHandler(failureHandler)
        .failureHandler(ErrorHandler.create(vertx));
    router.get(routing.getJwksUri().getPath()).handler(jwksRoute);
    router.get(routing.getIssuerPath().resolve(".well-known/*").getPath()).handler(wellKnownRoute);
    router.get(routing.getAuthorizationEndpoint().getPath()).handler(loginRoute);
    router
        .post(routing.getAuthenticationCallbackEndpoint(":sessionId").getPath())
        .handler(BodyHandler.create())
        .handler(authenticationRoute);
    router
        .post(routing.getTokenEndpoint().getPath())
        .handler(BodyHandler.create())
        .handler(tokenRoute);
    router.get(routing.getOpenIdPath("login-status-iframe.html*").getPath()).handler(iframeRoute);
    router
        .get(routing.getOpenIdPath("3p-cookies/step1.html").getPath())
        .handler(thirdPartyCookies1Route);
    router
        .get(routing.getOpenIdPath("3p-cookies/step2.html").getPath())
        .handler(thirdPartyCookies2Route);
    router.get(routing.getEndSessionEndpoint().getPath()).handler(logoutRoute);
    router.route("/auth/js/keycloak.js").handler(keycloakJsRoute);
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
      if (inputStream == null) {
        throw new IllegalStateException("Unable to find keystore in classpath");
      }
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
