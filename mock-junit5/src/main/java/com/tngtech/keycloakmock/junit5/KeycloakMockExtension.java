package com.tngtech.keycloakmock.junit5;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import com.tngtech.keycloakmock.api.KeycloakMock;
import com.tngtech.keycloakmock.api.ServerConfig;
import com.tngtech.keycloakmock.api.TokenConfig;
import com.tngtech.keycloakmock.impl.handler.TokenRoute;

/**
 * A JUnit5 extension to be used to automatically start and stop the keycloak mock.
 *
 * <p>Example use:
 *
 * <pre><code>
 * {@literal @}RegisterExtension
 *  static KeycloakMockExtension mock = new KeycloakMockExtension();
 *
 * {@literal @}Test
 *  void testStuff() {
 *    String token = mock.getAccessToken(aTokenConfig().build());
 *  }
 * </code></pre>
 */
public class KeycloakMockExtension implements BeforeAllCallback, AfterAllCallback {

  @Nonnull private final KeycloakMock mock;

  /**
   * Create a mock instance with default configuration.
   *
   * <p>The instance generates tokens for realm 'master'.
   *
   * <p>The JWKS endpoint listens at port 8000.
   *
   * <p>The JWKS endpoint is served via HTTP.
   *
   * @see KeycloakMockExtension#KeycloakMockExtension(ServerConfig)
   */
  public KeycloakMockExtension() {
    mock = new KeycloakMock();
  }

  /**
   * Create a mock instance for a given server configuration.
   *
   * @param serverConfig the port of the mock to run
   * @see KeycloakMockExtension#KeycloakMockExtension()
   */
  public KeycloakMockExtension(@Nonnull final ServerConfig serverConfig) {
    mock = new KeycloakMock(serverConfig);
  }

  /**
   * Get {@link TokenRoute} handler and control endpoit responses.
   *
   * <p>Example use:
   *
   * <pre><code>
   * {@literal //} return error 404
   * getTokenRoute().withErrorResponse(404, "{\"error\": \"Error detail message\"}")
   *
   * {@literal //} return 200
   * getTokenRoute().withOkResponse(accessTokenConfig, idTokenConfig, refreshTokenConfig, 60 * 60);
   * </code></pre>
   *
   * @return token route handler
   * @see TokenRoute
   */
  public TokenRoute getTokenRoute() {
    return mock.getTokenRoute();
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
    return mock.getAccessToken(tokenConfig);
  }

  @Override
  public void beforeAll(@Nullable final ExtensionContext context) {
    mock.start();
  }

  @Override
  public void afterAll(@Nullable final ExtensionContext context) {
    mock.stop();
  }
}
