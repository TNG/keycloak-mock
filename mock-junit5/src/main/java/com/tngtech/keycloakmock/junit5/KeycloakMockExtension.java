package com.tngtech.keycloakmock.junit5;

import com.tngtech.keycloakmock.api.KeycloakMock;
import com.tngtech.keycloakmock.api.ServerConfig;
import com.tngtech.keycloakmock.api.TokenConfig;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

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

  /**
   * Get the actual port that the mock is running on.
   *
   * @return the actual port
   * @throws IllegalStateException if the server is not running
   */
  public int getActualPort() {
    return mock.getActualPort();
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
