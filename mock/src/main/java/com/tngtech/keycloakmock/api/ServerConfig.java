package com.tngtech.keycloakmock.api;

import com.tngtech.keycloakmock.impl.Protocol;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;

/** Server configuration to use. */
public final class ServerConfig {

  private static final String DEFAULT_HOSTNAME = "localhost";
  private static final String DEFAULT_CONTEXT_PATH = "/auth";
  private static final int DEFAULT_PORT = 8000;

  /**
   * see {@link <a href="https://vertx.io/docs/vertx-core/java/#_listening_on_a_random_port">Vert.x
   * Core documentation</a>}
   */
  private static final int RANDOM_PORT = 0;

  private static final String DEFAULT_REALM = "master";
  private static final String DEFAULT_SCOPE = "openid";
  private static final Duration DEFAULT_TOKEN_LIFESPAN = Duration.ofHours(10);

  private final int port;
  @Nonnull private final Protocol protocol;
  @Nonnull private final String defaultHostname;
  @Nonnull private final String contextPath;
  @Nonnull private final String defaultRealm;
  @Nonnull private final List<String> defaultAudiences;
  @Nonnull private final List<String> defaultScopes;
  @Nonnull private final Duration defaultTokenLifespan;
  @Nonnull private final LoginRoleMapping loginRoleMapping;

  private ServerConfig(@Nonnull final Builder builder) {
    this.port = (builder.port > 0 ? builder.port : RANDOM_PORT);
    this.protocol = builder.protocol;
    this.defaultHostname = builder.defaultHostname;
    this.contextPath = builder.contextPath;
    this.defaultRealm = builder.defaultRealm;
    if (builder.defaultAudiences.isEmpty()) {
      this.defaultAudiences = Collections.singletonList("server");
    } else {
      this.defaultAudiences = builder.defaultAudiences;
    }
    this.defaultScopes = builder.defaultScopes;
    this.defaultTokenLifespan = builder.defaultTokenLifespan;
    this.loginRoleMapping = builder.loginRoleMapping;
  }

  /**
   * Get a new builder.
   *
   * @return a server configuration builder
   */
  @Nonnull
  public static Builder aServerConfig() {
    return new Builder();
  }

  /**
   * The port that the server is started on.
   *
   * @return the used port
   */
  public int getPort() {
    return port;
  }

  /**
   * The protocol that the server uses.
   *
   * @return the used protocol
   */
  @Nonnull
  public Protocol getProtocol() {
    return protocol;
  }

  /**
   * The audiences to add to the token by default.
   *
   * @return the list of default audiences
   */
  @Nonnull
  public List<String> getDefaultAudiences() {
    return Collections.unmodifiableList(defaultAudiences);
  }

  /**
   * The default hostname used in issuer claim.
   *
   * @return default hostname
   * @see TokenConfig.Builder#withHostname(String)
   */
  @Nonnull
  public String getDefaultHostname() {
    return defaultHostname;
  }

  /**
   * Keycloak context path.
   *
   * @return context path
   * @see Builder#withContextPath(String)
   * @see Builder#withNoContextPath()
   */
  @Nonnull
  public String getContextPath() {
    return contextPath;
  }

  /**
   * The default realm used in issuer claim.
   *
   * @return default realm
   */
  @Nonnull
  public String getDefaultRealm() {
    return defaultRealm;
  }

  /**
   * The default scopes used in scope claim.
   *
   * @return default scopes
   */
  @Nonnull
  public List<String> getDefaultScopes() {
    return Collections.unmodifiableList(defaultScopes);
  }

  /**
   * Get default access token lifespan.
   *
   * @return default token lifespan
   */
  @Nonnull
  public Duration getDefaultTokenLifespan() {
    return defaultTokenLifespan;
  }

  /**
   * Get mapping logic for roles passed through login page.
   *
   * @return login role mapping
   */
  @Nonnull
  public LoginRoleMapping getLoginRoleMapping() {
    return loginRoleMapping;
  }

  /**
   * Builder for {@link ServerConfig}.
   *
   * <p>Use this to generate a server configuration to your needs.
   */
  public static final class Builder {

    private int port = DEFAULT_PORT;
    @Nonnull private Protocol protocol = Protocol.HTTP;
    @Nonnull private String defaultHostname = DEFAULT_HOSTNAME;
    @Nonnull private String contextPath = DEFAULT_CONTEXT_PATH;
    @Nonnull private String defaultRealm = DEFAULT_REALM;
    @Nonnull private final List<String> defaultAudiences = new ArrayList<>();
    @Nonnull private final List<String> defaultScopes = new ArrayList<>();
    @Nonnull private Duration defaultTokenLifespan = DEFAULT_TOKEN_LIFESPAN;
    @Nonnull private LoginRoleMapping loginRoleMapping = LoginRoleMapping.TO_REALM;

    private Builder() {
      defaultScopes.add(DEFAULT_SCOPE);
    }

    /**
     * Set TLS flag.
     *
     * <p>If set to true, start the server with TLS. Default value is false.
     *
     * @param tls the flag to use
     * @return builder
     */
    @Nonnull
    public Builder withTls(final boolean tls) {
      this.protocol = tls ? Protocol.HTTPS : Protocol.HTTP;
      return this;
    }

    /**
     * Set port.
     *
     * <p>The port that the server is started on. Default value is 8000.
     *
     * @param port the port to use
     * @return builder
     */
    @Nonnull
    public Builder withPort(final int port) {
      this.port = port;
      return this;
    }

    /**
     * Use random port.
     *
     * <p>Will start the server on a random port. Actual value can be retrieved via {@link
     * KeycloakMock#getActualPort()}.
     */
    @Nonnull
    public Builder withRandomPort() {
      this.port = RANDOM_PORT;
      return this;
    }

    /**
     * Set default hostname.
     *
     * <p>The hostname that is used as token issuer if no explicit hostname is configured for the
     * token. Default value is 'localhost'.
     *
     * @param defaultHostname the hostname to use
     * @return builder
     * @see TokenConfig.Builder#withHostname(String)
     */
    @Nonnull
    public Builder withDefaultHostname(@Nonnull final String defaultHostname) {
      this.defaultHostname = defaultHostname;
      return this;
    }

    /**
     * Set default realm.
     *
     * <p>The realm that is used in issued tokens if no explicit realm is configured for the token.
     * Default value is 'master'.
     *
     * @param defaultRealm the realm to use
     * @return builder
     * @see TokenConfig.Builder#withRealm(String)
     */
    @Nonnull
    public Builder withDefaultRealm(@Nonnull final String defaultRealm) {
      this.defaultRealm = defaultRealm;
      return this;
    }

    /**
     * Add default audiences.
     *
     * <p>The audience that is issued in tokens if no explicit audience is configured for the token.
     *
     * <p>If no default audience is set, it will default to 'server'.
     *
     * @param audiences the audiences to add
     * @return builder
     * @see #withDefaultAudience(String)
     * @see TokenConfig.Builder#withAudience(String)
     * @see TokenConfig.Builder#withAudiences(Collection)
     */
    @Nonnull
    public Builder withDefaultAudiences(@Nonnull Collection<String> audiences) {
      defaultAudiences.addAll(audiences);
      return this;
    }

    /**
     * Add a default audience.
     *
     * <p>The audience that is issued in tokens if no explicit audience is configured for the token.
     *
     * <p>If no default audience is set, it will default to 'server'.
     *
     * @param resource an audience to add
     * @return builder
     * @see #withDefaultAudiences(Collection) (Collection)
     * @see TokenConfig.Builder#withAudience(String)
     * @see TokenConfig.Builder#withAudiences(Collection)
     */
    @SuppressWarnings("unused")
    @Nonnull
    public Builder withDefaultAudience(@Nonnull String resource) {
      defaultAudiences.add(Objects.requireNonNull(resource));
      return this;
    }

    /**
     * Set context path.
     *
     * <p>Before quarkus based Keycloak distribution /auth prefix was obligatory. Now /auth prefix
     * is removed and can be enabled/overridden in configuration to keep backward compatibility.
     * Default value is '/auth' To disable context path use {@link #withNoContextPath()} method.
     *
     * @see <a href="https://www.keycloak.org/server/all-config#category-hostname">hostname-path</a>
     * @see <a
     *     href="https://www.keycloak.org/migration/migrating-to-quarkus#_default_context_path_changed">Default
     *     context path changed</a>
     * @param contextPath context path to use
     * @return builder
     */
    @Nonnull
    public Builder withContextPath(@Nonnull String contextPath) {
      this.contextPath = contextPath;
      return this;
    }

    /**
     * Disabling context path.
     *
     * @see #withContextPath(String)
     * @return builder
     */
    @Nonnull
    public Builder withNoContextPath() {
      this.contextPath = "";
      return this;
    }

    /**
     * Set default client scopes.
     *
     * <p>Set of client scopes to be configured. Default scope 'openid' is always added.
     *
     * @param defaultScopes the scopes to add
     * @return builder
     * @see <a href="https://openid.net/specs/openid-connect-core-1_0.html#ScopeClaims">scope
     *     claims</a>
     */
    @Nonnull
    public Builder withDefaultScopes(@Nonnull final Collection<String> defaultScopes) {
      this.defaultScopes.addAll(defaultScopes);
      return this;
    }

    /**
     * Set default client scope.
     *
     * <p>A client scope to be configured. Default scope 'openid' is always added.
     *
     * @param defaultScope as string
     * @return builder
     * @see <a href="https://openid.net/specs/openid-connect-core-1_0.html#ScopeClaims">scope
     *     claims</a>
     */
    @Nonnull
    public Builder withDefaultScope(@Nonnull final String defaultScope) {
      this.defaultScopes.add(defaultScope);
      return this;
    }

    /**
     * Set default access token lifespan.
     *
     * <p>Token expiry 'exp' will be set as 'issuedAt' + 'tokenLifespan'. The default lifespan is 10
     * hours.
     *
     * @param tokenLifespan as duration
     * @return builder
     */
    @Nonnull
    public Builder withDefaultTokenLifespan(@Nonnull final Duration tokenLifespan) {
      this.defaultTokenLifespan = tokenLifespan;
      return this;
    }

    /**
     * Set the role mapping to use for the login route.
     *
     * <p>When using the login flow, the roles can only be given as a list. This setting allows
     * specifying where they should be applied.
     *
     * <p>The default setting is {@link LoginRoleMapping#TO_REALM}.
     *
     * <p>This setting only applies when using the login flow via Browser, not when generating
     * tokens programmatically.
     *
     * @param loginRoleMapping the role mapping
     * @return builder
     */
    @Nonnull
    public Builder withLoginRoleMapping(@Nonnull final LoginRoleMapping loginRoleMapping) {
      this.loginRoleMapping = loginRoleMapping;
      return this;
    }

    /**
     * Build the server configuration.
     *
     * @return the server configuration
     */
    @Nonnull
    public ServerConfig build() {
      return new ServerConfig(this);
    }
  }
}
