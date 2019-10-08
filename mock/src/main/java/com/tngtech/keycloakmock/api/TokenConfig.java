package com.tngtech.keycloakmock.api;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The configuration from which to generate an access token.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * TokenConfig config = TokenConfig.aTokenConfig().withSubject("subject).build();
 * }</pre>
 */
public class TokenConfig {
  @Nonnull private final Set<String> audience;
  @Nonnull private final String authorizedParty;
  @Nonnull private final String subject;
  @Nonnull private final Map<String, Object> claims;
  @Nonnull private final Access realmAccess;
  @Nonnull private final Map<String, Access> resourceAccess;
  @Nonnull private final Instant issuedAt;
  @Nonnull private final Instant expiration;
  @Nullable private final String name;
  @Nullable private final String givenName;
  @Nullable private final String familyName;
  @Nullable private final String email;
  @Nullable private final String preferredUsername;

  private TokenConfig(@Nonnull final Builder builder) {
    if (builder.audience.isEmpty()) {
      audience = Collections.singleton("server");
    } else {
      audience = builder.audience;
    }
    authorizedParty = builder.authorizedParty;
    subject = builder.subject;
    claims = builder.claims;
    realmAccess = builder.realmRoles;
    resourceAccess = builder.resourceAccess;
    issuedAt = builder.issuedAt;
    expiration = builder.expiration;
    givenName = builder.givenName;
    familyName = builder.familyName;
    if (givenName != null) {
      if (familyName != null) {
        name = givenName + " " + familyName;
      } else {
        name = givenName;
      }
    } else {
      name = familyName;
    }
    email = builder.email;
    preferredUsername = builder.preferredUsername;
  }

  /**
   * Get a new builder.
   *
   * @return a token config builder
   */
  @Nonnull
  public static Builder aTokenConfig() {
    return new Builder();
  }

  @Nonnull
  public Set<String> getAudience() {
    return Collections.unmodifiableSet(audience);
  }

  @Nonnull
  public String getAuthorizedParty() {
    return authorizedParty;
  }

  @Nonnull
  public String getSubject() {
    return subject;
  }

  @Nonnull
  public Map<String, Object> getClaims() {
    return Collections.unmodifiableMap(claims);
  }

  @Nonnull
  public Access getRealmAccess() {
    return realmAccess;
  }

  @Nonnull
  public Map<String, Access> getResourceAccess() {
    return Collections.unmodifiableMap(resourceAccess);
  }

  @Nonnull
  public Instant getIssuedAt() {
    return issuedAt;
  }

  @Nonnull
  public Instant getExpiration() {
    return expiration;
  }

  @Nullable
  public String getName() {
    return name;
  }

  @Nullable
  public String getGivenName() {
    return givenName;
  }

  @Nullable
  public String getFamilyName() {
    return familyName;
  }

  @Nullable
  public String getEmail() {
    return email;
  }

  @Nullable
  public String getPreferredUsername() {
    return preferredUsername;
  }

  /**
   * Builder for {@link TokenConfig}.
   *
   * <p>Use this to generate a token configuration to your needs.
   */
  public static final class Builder {
    @Nonnull private Set<String> audience = new HashSet<>();
    @Nonnull private String authorizedParty = "client";
    @Nonnull private String subject = "user";
    @Nonnull private Map<String, Object> claims = new HashMap<>();
    @Nonnull private Access realmRoles = new Access();
    @Nonnull private Map<String, Access> resourceAccess = new HashMap<>();
    @Nonnull private Instant issuedAt = Instant.now();
    @Nonnull private Instant expiration = issuedAt.plus(10, ChronoUnit.HOURS);
    @Nullable private String givenName;
    @Nullable private String familyName;
    @Nullable private String email;
    @Nullable private String preferredUsername;

    private Builder() {}

    /**
     * Add an audience.
     *
     * <p>An audience is an identifier of a recipient of the token.
     *
     * @param audience the audience to add
     * @return builder
     * @see <a href="https://openid.net/specs/openid-connect-core-1_0.html#IDToken" />
     */
    @Nonnull
    public Builder withAudience(@Nonnull String audience) {
      this.audience.add(Objects.requireNonNull(audience));
      return this;
    }

    /**
     * Add a collection of audiences.
     *
     * <p>An audience is an identifier of a recipient of the token.
     *
     * @param audiences the audiences to add
     * @return builder
     * @see <a href="https://openid.net/specs/openid-connect-core-1_0.html#IDToken" />
     */
    @Nonnull
    public Builder withAudiences(@Nonnull Collection<String> audiences) {
      this.audience.addAll(Objects.requireNonNull(audience));
      return this;
    }

    /**
     * Set authorized party.
     *
     * <p>The authorized party identifies the party for which the token was issued.
     *
     * @param authorizedParty the authorized party to set
     * @return builder
     * @see <a href="https://openid.net/specs/openid-connect-core-1_0.html#IDToken" />
     */
    @Nonnull
    public Builder withAuthorizedParty(@Nonnull String authorizedParty) {
      this.authorizedParty = Objects.requireNonNull(authorizedParty);
      return this;
    }

    /**
     * Set subject.
     *
     * <p>The subject identifies the end-user for which the token was issued.
     *
     * @param subject the subject to set
     * @return builder
     * @see <a href="https://openid.net/specs/openid-connect-core-1_0.html#IDToken" />
     */
    @Nonnull
    public Builder withSubject(@Nonnull String subject) {
      this.subject = Objects.requireNonNull(subject);
      return this;
    }

    /**
     * Add realm roles.
     *
     * <p>Realm roles apply to all clients within a realm.
     *
     * @param roles the roles to add
     * @return builder
     * @see <a href="https://www.keycloak.org/docs/latest/server_admin/index.html#realm-roles" />
     */
    @Nonnull
    public Builder withRealmRoles(@Nonnull Collection<String> roles) {
      this.realmRoles.addRoles(Objects.requireNonNull(roles));
      return this;
    }

    /**
     * Add a realm role
     *
     * <p>Realm roles apply to all clients within a realm.
     *
     * @param role the role to add
     * @return builder
     * @see <a href="https://www.keycloak.org/docs/latest/server_admin/index.html#realm-roles" />
     */
    @Nonnull
    public Builder withRealmRole(@Nonnull String role) {
      this.realmRoles.addRole(Objects.requireNonNull(role));
      return this;
    }

    /**
     * Add resource roles.
     *
     * <p>Resource roles only apply to a specific client or resource.
     *
     * @param resource the resource or client for which to add the roles
     * @param roles the roles to add
     * @return builder
     * @see <a href="https://www.keycloak.org/docs/latest/server_admin/index.html#client-roles" />
     */
    @Nonnull
    public Builder withResourceRoles(
        @Nonnull final String resource, @Nonnull Collection<String> roles) {
      this.resourceAccess
          .computeIfAbsent(Objects.requireNonNull(resource), k -> new Access())
          .addRoles(Objects.requireNonNull(roles));
      return this;
    }

    /**
     * Add a resource role.
     *
     * <p>Resource roles only apply to a specific client or resource.
     *
     * @param resource the resource or client for which to add the roles
     * @param role the role to add
     * @return builder
     * @see <a href="https://www.keycloak.org/docs/latest/server_admin/index.html#client-roles" />
     */
    @Nonnull
    public Builder withResourceRole(@Nonnull final String resource, @Nonnull String role) {
      this.resourceAccess
          .computeIfAbsent(Objects.requireNonNull(resource), k -> new Access())
          .addRole(Objects.requireNonNull(role));
      return this;
    }

    /**
     * Add generic claims.
     *
     * <p>Use this method to add elements to the token cannot be set using more specialized methods.
     * The underlying library uses <a href="https://github.com/FasterXML/jackson-databind">Jackson
     * data-binding</a>, so getters will be used to generate JSON objects.
     *
     * @param claims the claims to add (map from claim name to claim value)
     * @return builder
     */
    @Nonnull
    public Builder withClaims(@Nonnull Map<String, Object> claims) {
      this.claims.putAll(Objects.requireNonNull(claims));
      return this;
    }

    /**
     * Add generic claim.
     *
     * <p>Use this method to add elements to the token cannot be set using more specialized methods.
     * The underlying library uses <a href="https://github.com/FasterXML/jackson-databind">Jackson
     * data-binding</a>, so getters will be used to generate JSON objects.
     *
     * @param key the claim name
     * @param value the claim value
     * @return builder
     */
    @Nonnull
    public Builder withClaim(@Nonnull String key, @Nonnull Object value) {
      this.claims.put(Objects.requireNonNull(key), Objects.requireNonNull(value));
      return this;
    }

    /**
     * Set issued at date.
     *
     * @param issuedAt the instant when the token was generated
     * @return builder
     * @see <a href="https://openid.net/specs/openid-connect-core-1_0.html#IDToken" />
     */
    @Nonnull
    public Builder withIssuedAt(@Nonnull Instant issuedAt) {
      this.issuedAt = Objects.requireNonNull(issuedAt);
      return this;
    }

    /**
     * Set expiration date.
     *
     * @param expiration the instant when the token expires
     * @return builder
     * @see <a href="https://openid.net/specs/openid-connect-core-1_0.html#IDToken" />
     */
    @Nonnull
    public Builder withExpiration(@Nonnull Instant expiration) {
      this.expiration = Objects.requireNonNull(expiration);
      return this;
    }

    /**
     * Set given name.
     *
     * @param givenName the given name of the user
     * @return builder
     * @see <a href="https://www.keycloak.org/docs/latest/server_admin/index.html#_create-new-user"
     *     />
     */
    @Nonnull
    public Builder withGivenName(@Nullable final String givenName) {
      this.givenName = givenName;
      return this;
    }

    /**
     * Set family name.
     *
     * @param familyName the family name of the user
     * @return builder
     * @see <a href="https://www.keycloak.org/docs/latest/server_admin/index.html#_create-new-user"
     *     />
     */
    @Nonnull
    public Builder withFamilyName(@Nullable final String familyName) {
      this.familyName = familyName;
      return this;
    }

    /**
     * Set email address.
     *
     * @param email the email address of the user
     * @return builder
     * @see <a href="https://www.keycloak.org/docs/latest/server_admin/index.html#_create-new-user"
     *     />
     */
    @Nonnull
    public Builder withEmail(@Nullable final String email) {
      this.email = email;
      return this;
    }

    /**
     * Set preferred username.
     *
     * @param preferredUsername the preferred username of the user
     * @return builder
     * @see <a href="https://www.keycloak.org/docs/latest/server_admin/index.html#_create-new-user"
     *     />
     */
    @Nonnull
    public Builder withPreferredUsername(@Nullable final String preferredUsername) {
      this.preferredUsername = preferredUsername;
      return this;
    }

    @Nonnull
    public TokenConfig build() {
      return new TokenConfig(this);
    }
  }

  static class Access {
    @Nonnull private final Set<String> roles = new HashSet<>();

    @Nonnull
    public Set<String> getRoles() {
      return Collections.unmodifiableSet(roles);
    }

    void addRole(@Nonnull final String role) {
      roles.add(Objects.requireNonNull(role));
    }

    void addRoles(@Nonnull final Collection<String> newRoles) {
      roles.addAll(newRoles);
    }
  }
}
