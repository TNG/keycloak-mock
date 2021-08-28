package com.tngtech.keycloakmock.api;

import com.tngtech.keycloakmock.impl.Protocol;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;

/** Server configuration to use. */
public final class ServerConfig {

  private static final String DEFAULT_HOSTNAME = "localhost";
  private static final int DEFAULT_PORT = 8000;
  private static final String DEFAULT_REALM = "master";

  private final int port;
  @Nonnull private final Protocol protocol;
  @Nonnull private final String defaultHostname;
  @Nonnull private final String defaultRealm;
  @Nonnull private final List<String> resourcesToMapRolesTo;

  private ServerConfig(@Nonnull final Builder builder) {
    this.port = builder.port;
    this.protocol = builder.protocol;
    this.defaultHostname = builder.defaultHostname;
    this.defaultRealm = builder.defaultRealm;
    this.resourcesToMapRolesTo = builder.resourcesToMapRolesTo;
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
   * The resources for which roles will be set.
   *
   * <p>If this list is empty, a login via the built-in login page will take the comma-separated
   * roles from the password field and assign it as realm roles. If it contains at least one
   * resource, the roles will be mapped only to this resource.
   *
   * @return the list of resources to which roles are mapped
   * @see <a href="https://www.keycloak.org/docs/latest/server_admin/#realm-roles">Realm Roles</a>
   * @see <a href="https://www.keycloak.org/docs/latest/server_admin/#client-roles">Client Roles</a>
   */
  @Nonnull
  public List<String> getResourcesToMapRolesTo() {
    return Collections.unmodifiableList(resourcesToMapRolesTo);
  }

  /**
   * The default hostname used in issuer claim.
   *
   * @return default hostname
   * @deprecated use {@link #getDefaultHostname()} instead
   */
  @Nonnull
  @Deprecated
  public String getHostname() {
    return getDefaultHostname();
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
   * The default realm used in issuer claim.
   *
   * @return default realm
   * @deprecated use {@link #getDefaultRealm()} instead
   */
  @Nonnull
  @Deprecated
  public String getRealm() {
    return getDefaultRealm();
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

  public static final class Builder {

    private int port = DEFAULT_PORT;
    @Nonnull private Protocol protocol = Protocol.HTTP;
    @Nonnull private String defaultHostname = DEFAULT_HOSTNAME;
    @Nonnull private String defaultRealm = DEFAULT_REALM;
    @Nonnull private final List<String> resourcesToMapRolesTo = new ArrayList<>();

    private Builder() {}

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
     * Set default hostname.
     *
     * <p>The hostname that is used as token issuer if no explicit hostname is configured for the
     * token. Default value is 'localhost'.
     *
     * @param defaultHostname the hostname to use
     * @return builder
     * @see TokenConfig.Builder#withHostname(String)
     * @deprecated use {@link #withDefaultHostname(String)} instead
     */
    @Nonnull
    @Deprecated
    public Builder withHostname(@Nonnull final String defaultHostname) {
      return withDefaultHostname(defaultHostname);
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
     * @deprecated use {@link #withDefaultRealm(String)} instead
     */
    @Nonnull
    @Deprecated
    public Builder withRealm(@Nonnull final String defaultRealm) {
      return withDefaultRealm(defaultRealm);
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
     * Set resources for which roles will be set.
     *
     * <p>If this list is empty, a login via the built-in login page will take the comma-separated
     * roles from the password field and assign it as realm roles. If it contains at least one
     * resource, the roles will be mapped only to this resource.
     *
     * @param resources the list of resources to which roles will be mapped
     * @return builder
     * @see #withResourceToMapRolesTo(String)
     * @see <a href="https://www.keycloak.org/docs/latest/server_admin/#realm-roles">Realm Roles</a>
     * @see <a href="https://www.keycloak.org/docs/latest/server_admin/#client-roles">Client
     *     Roles</a>
     */
    @Nonnull
    public Builder withResourcesToMapRolesTo(@Nonnull List<String> resources) {
      resourcesToMapRolesTo.addAll(resources);
      return this;
    }

    /**
     * Add a resource for which roles will be set.
     *
     * <p>If no resource is set, a login via the built-in login page will take the comma-separated
     * roles from the password field and assign it as realm roles. If at least one resource is
     * configured, the roles will be mapped only to this resource.
     *
     * @param resource a resource to which roles will be mapped
     * @return builder
     * @see #withResourcesToMapRolesTo(List)
     * @see <a href="https://www.keycloak.org/docs/latest/server_admin/#realm-roles">Realm Roles</a>
     * @see <a href="https://www.keycloak.org/docs/latest/server_admin/#client-roles">Client
     *     Roles</a>
     */
    @Nonnull
    public Builder withResourceToMapRolesTo(@Nonnull String resource) {
      resourcesToMapRolesTo.add(Objects.requireNonNull(resource));
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
