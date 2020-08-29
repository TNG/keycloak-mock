package com.tngtech.keycloakmock.junit;

import com.tngtech.keycloakmock.api.TokenConfig;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.rules.ExternalResource;

/**
 * A JUnit helper to automatically start and stop the keycloak mock.
 *
 * <p>Example JUnit4 use:
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
 *
 * <p>Example JUnit5 use:
 *
 * <pre><code>
 * {@literal @}RegisterExtension
 *  static KeycloakMock mock = new KeycloakMock();
 *
 * {@literal @}Test
 *  void testStuff() {
 *    String token = mock.getAccessToken(aTokenConfig().build());
 *  }
 * </code></pre>
 */
public class KeycloakMock extends ExternalResource implements BeforeAllCallback, AfterAllCallback {

  @Nonnull private final com.tngtech.keycloakmock.api.KeycloakMock mock;

  /**
   * Create a mock instance for a given realm.
   *
   * <p>The instance generates tokens for realm 'master'. If you want to use a different realm, use
   * {@link KeycloakMock#KeycloakMock(int, String)} instead.
   *
   * <p>The JWKS endpoint listens at port 8000. If you need a different port, use {@link
   * KeycloakMock#KeycloakMock(int, String)} instead.
   *
   * <p>The JWKS endpoint is served via HTTP. If you need HTTPS, use {@link
   * KeycloakMock#KeycloakMock(int, String, boolean)} instead.
   */
  public KeycloakMock() {
    this(8000, "master", false);
  }

  /**
   * Create a mock instance for a given realm.
   *
   * <p>The JWKS endpoint is served via HTTP. If you need HTTPS, use {@link
   * KeycloakMock#KeycloakMock(int, String, boolean)} instead.
   *
   * @param port the port of the mock to run
   * @param realm the realm for which to provide tokens
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
   */
  public KeycloakMock(final int port, @Nonnull final String realm, final boolean tls) {
    this.mock = new com.tngtech.keycloakmock.api.KeycloakMock(port, realm, tls);
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

  @Override
  public void beforeAll(@Nonnull final ExtensionContext context) {
    mock.start();
  }

  @Override
  public void afterAll(@Nonnull final ExtensionContext context) {
    mock.stop();
  }
}
