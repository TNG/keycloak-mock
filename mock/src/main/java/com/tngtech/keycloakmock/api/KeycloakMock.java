package com.tngtech.keycloakmock.api;

import com.tngtech.keycloakmock.impl.UrlConfiguration;
import com.tngtech.keycloakmock.impl.UrlConfigurationFactory;
import com.tngtech.keycloakmock.impl.dagger.DaggerServerComponent;
import com.tngtech.keycloakmock.impl.dagger.DaggerSignatureComponent;
import com.tngtech.keycloakmock.impl.dagger.ServerComponent;
import com.tngtech.keycloakmock.impl.dagger.SignatureComponent;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServer;
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
 * com.tngtech.keycloakmock.junit.KeycloakMockRule} from module mock-junit or {@code
 * com.tngtech.keycloakmock.junit5.KeycloakMockExtension} instead.
 */
public class KeycloakMock {
  private static final Logger LOG = LoggerFactory.getLogger(KeycloakMock.class);

  @Nonnull private final ServerConfig serverConfig;
  @Nonnull private final UrlConfigurationFactory urlConfigurationFactory;

  @Nonnull private final SignatureComponent signatureComponent;

  @Nullable private ServerComponent serverComponent;

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
    this.serverConfig = serverConfig;
    this.urlConfigurationFactory = new UrlConfigurationFactory(serverConfig);
    this.signatureComponent =
        DaggerSignatureComponent.builder()
            .defaultScopes(serverConfig.getDefaultScopes())
            .defaultAudiences(serverConfig.getDefaultAudiences())
            .defaultTokenLifespan(serverConfig.getDefaultTokenLifespan())
            .build();
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
    UrlConfiguration configuration =
        urlConfigurationFactory.create(tokenConfig.getHostname(), tokenConfig.getRealm());
    return signatureComponent.tokenGenerator().getToken(tokenConfig, configuration);
  }

  /**
   * Start the server (blocking).
   *
   * @throws MockServerException when the server could not be started properly
   * @throws IllegalStateException when the built-in keystore could not be read for TLS mode
   */
  public synchronized void start() {
    if (serverComponent != null) {
      LOG.warn("Start request ignored as server is already running");
      return;
    }
    ResultHandler<HttpServer> startHandler = new ResultHandler<>();
    serverComponent =
        DaggerServerComponent.builder()
            .serverConfig(serverConfig)
            .publicKey(signatureComponent.publicKey())
            .keyId(signatureComponent.keyId())
            .keyStore(signatureComponent.keyStore())
            .tokenGenerator(signatureComponent.tokenGenerator())
            .build();
    serverComponent.server().listen(startHandler);
    startHandler.await();
  }

  /**
   * Stop the server (blocking).
   *
   * @throws MockServerException when the server could not be stopped properly
   */
  public synchronized void stop() {
    if (serverComponent != null) {
      ResultHandler<Void> stopServerHandler = new ResultHandler<>();
      serverComponent.server().close(stopServerHandler);
      stopServerHandler.await();
      ResultHandler<Void> stopVertxHandler = new ResultHandler<>();
      serverComponent.vertx().close(stopVertxHandler);
      stopVertxHandler.await();
      serverComponent = null;
    }
  }

  /**
   * Return the actual port the server is running on.
   *
   * @return the port
   * @throws IllegalStateException if the server is not running
   */
  public synchronized int getActualPort() {
    if (serverComponent == null) {
      throw new IllegalStateException("Server is not running!");
    }
    return serverComponent.server().actualPort();
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
