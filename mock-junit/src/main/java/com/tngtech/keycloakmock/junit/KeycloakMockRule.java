package com.tngtech.keycloakmock.junit;

import com.tngtech.keycloakmock.api.KeycloakMock;
import com.tngtech.keycloakmock.api.ServerConfig;
import com.tngtech.keycloakmock.api.TokenConfig;
import javax.annotation.Nonnull;
import org.junit.rules.ExternalResource;

/**
 * A JUnit4 rule to automatically start and stop the keycloak mock.
 *
 * <p>Example use:
 *
 * <pre><code>
 * {@literal @}ClassRule
 *  public static KeycloakMockRule mock = new KeycloakMockRule();
 *
 * {@literal @}Test
 *  public void testStuff() {
 *    String token = mock.getAccessToken(aTokenConfig().build());
 *  }
 * </code></pre>
 */
public class KeycloakMockRule extends ExternalResource {

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
   * @see KeycloakMockRule#KeycloakMockRule(ServerConfig)
   */
  public KeycloakMockRule() {
    mock = new KeycloakMock();
  }

  /**
   * Create a mock instance for a given server configuration.
   *
   * @param serverConfig the port of the mock to run
   * @see KeycloakMockRule#KeycloakMockRule()
   */
  public KeycloakMockRule(@Nonnull final ServerConfig serverConfig) {
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
  protected void before() {
    mock.start();
  }

  @Override
  protected void after() {
    mock.stop();
  }
}
