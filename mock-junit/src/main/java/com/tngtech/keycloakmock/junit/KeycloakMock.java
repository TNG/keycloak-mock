package com.tngtech.keycloakmock.junit;

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
 *  public static KeycloakMock mock = new KeycloakMock();
 *
 * {@literal @}Test
 *  public void testStuff() {
 *    String token = mock.getAccessToken(aTokenConfig().build());
 *  }
 * </code></pre>
 */
public class KeycloakMock extends ExternalResource {

  @Nonnull private final com.tngtech.keycloakmock.api.KeycloakMock mock;

  /**
   * Create a mock instance with default configuration.
   *
   * <p>The instance generates tokens for realm 'master'.
   *
   * <p>The JWKS endpoint listens at port 8000.
   *
   * <p>The JWKS endpoint is served via HTTP.
   *
   * @see KeycloakMock#KeycloakMock(ServerConfig)
   */
  public KeycloakMock() {
    mock = new com.tngtech.keycloakmock.api.KeycloakMock();
  }

  /**
   * Create a mock instance for a given server configuration.
   *
   * @param serverConfig the port of the mock to run
   * @see KeycloakMock#KeycloakMock()
   */
  public KeycloakMock(@Nonnull final ServerConfig serverConfig) {
    mock = new com.tngtech.keycloakmock.api.KeycloakMock(serverConfig);
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
  protected void before() {
    mock.start();
  }

  @Override
  protected void after() {
    mock.stop();
  }
}
