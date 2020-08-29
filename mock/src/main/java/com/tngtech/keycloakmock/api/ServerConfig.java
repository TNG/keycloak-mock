package com.tngtech.keycloakmock.api;

import javax.annotation.Nonnull;

/** Server configuration to use. */
public final class ServerConfig {

  private static final String DEFAULT_HOSTNAME = "localhost";
  private static final int DEFAULT_PORT = 8000;
  private static final String DEFAULT_REALM = "master";

  @Nonnull private final String hostname;
  private final int port;
  @Nonnull private final String realm;
  private final boolean tls;

  private ServerConfig(@Nonnull final Builder builder) {
    this.hostname = builder.hostname;
    this.port = builder.port;
    this.realm = builder.realm;
    this.tls = builder.tls;
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

  @Nonnull
  public String getHostname() {
    return hostname;
  }

  public int getPort() {
    return port;
  }

  @Nonnull
  public String getRealm() {
    return realm;
  }

  public boolean isTls() {
    return tls;
  }

  public static final class Builder {

    @Nonnull private String hostname = DEFAULT_HOSTNAME;
    private int port = DEFAULT_PORT;
    @Nonnull private String realm = DEFAULT_REALM;
    private boolean tls;

    private Builder() {}

    /**
     * Set hostname.
     *
     * <p>The hostname that is used as token issuer. Default value is 'localhost'.
     *
     * @param hostname the hostname to use
     * @return builder
     */
    @Nonnull
    public Builder withHostname(@Nonnull final String hostname) {
      this.hostname = hostname;
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
     * Set realm.
     *
     * <p>The realm that is used in issued tokens. Default value is 'master'.
     *
     * @param realm the realm to use
     * @return builder
     */
    @Nonnull
    public Builder withRealm(@Nonnull final String realm) {
      this.realm = realm;
      return this;
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
      this.tls = tls;
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
