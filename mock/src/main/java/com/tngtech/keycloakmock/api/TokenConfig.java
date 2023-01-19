package com.tngtech.keycloakmock.api;

import io.jsonwebtoken.ClaimJwtException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
  private static final Pattern ISSUER_PATH_PATTERN = Pattern.compile("^/auth/realms/([^/]+)$");

  @Nonnull private final Set<String> audience;
  @Nonnull private final String authorizedParty;
  @Nonnull private final String subject;
  @Nonnull private final String scope;
  @Nonnull private final Map<String, Object> claims;
  @Nonnull private final Access realmAccess;
  @Nonnull private final Map<String, Access> resourceAccess;
  @Nonnull private final Instant issuedAt;
  @Nonnull private final Instant authenticationTime;
  @Nonnull private final Instant expiration;
  @Nullable private final Instant notBefore;
  @Nullable private final String hostname;
  @Nullable private final String realm;
  @Nullable private final String name;
  @Nullable private final String givenName;
  @Nullable private final String familyName;
  @Nullable private final String email;
  @Nullable private final String preferredUsername;
  @Nullable private final String authenticationContextClassReference;

  private TokenConfig(@Nonnull final Builder builder) {
    if (builder.audience.isEmpty()) {
      audience = Collections.singleton("server");
    } else {
      audience = builder.audience;
    }
    authorizedParty = builder.authorizedParty;
    subject = builder.subject;
    scope = String.join(" ", builder.scope);
    claims = builder.claims;
    realmAccess = builder.realmRoles;
    resourceAccess = builder.resourceAccess;
    issuedAt = builder.issuedAt;
    authenticationTime = builder.authenticationTime;
    expiration = builder.expiration;
    hostname = builder.hostname;
    realm = builder.realm;
    notBefore = builder.notBefore;
    givenName = builder.givenName;
    familyName = builder.familyName;
    if (builder.name != null) {
      name = builder.name;
    } else if (givenName != null) {
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
    authenticationContextClassReference = builder.authenticationContextClassReference;
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
  public String getScope() {
    return scope;
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
  public Instant getAuthenticationTime() {
    return authenticationTime;
  }

  @Nonnull
  public Instant getExpiration() {
    return expiration;
  }

  @Nullable
  public Instant getNotBefore() {
    return notBefore;
  }

  @Nullable
  public String getHostname() {
    return hostname;
  }

  @Nullable
  public String getRealm() {
    return realm;
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

  @Nullable
  public String getAuthenticationContextClassReference() {
    return authenticationContextClassReference;
  }

  /**
   * Builder for {@link TokenConfig}.
   *
   * <p>Use this to generate a token configuration to your needs.
   */
  public static final class Builder {

    @Nonnull private final Set<String> audience = new HashSet<>();
    @Nonnull private String authorizedParty = "client";
    @Nonnull private String subject = "user";
    @Nonnull private final Set<String> scope = new HashSet<>();
    @Nonnull private final Map<String, Object> claims = new HashMap<>();
    @Nonnull private final Access realmRoles = new Access();
    @Nonnull private final Map<String, Access> resourceAccess = new HashMap<>();
    @Nonnull private Instant issuedAt = Instant.now();
    @Nonnull private Instant expiration = issuedAt.plus(10, ChronoUnit.HOURS);
    @Nonnull private Instant authenticationTime = Instant.now();
    @Nullable private Instant notBefore;
    @Nullable private String hostname;
    @Nullable private String realm;
    @Nullable private String givenName;
    @Nullable private String familyName;
    @Nullable private String name;
    @Nullable private String email;
    @Nullable private String preferredUsername;
    @Nullable private String authenticationContextClassReference;

    private Builder() {}

    /**
     * Add the contents of a real token.
     *
     * <p>The content of the token is read into this token config. Signature, issuer as well as
     * start and end time of the original token are ignored.
     *
     * @param originalToken token to read from
     * @return builder
     */
    @Nonnull
    @SuppressWarnings("unchecked")
    public Builder withSourceToken(@Nonnull final String originalToken) {
      int i = originalToken.lastIndexOf('.');
      String untrustedJwtString = originalToken.substring(0, i + 1);
      Claims untrustedClaims;
      try {
        untrustedClaims = Jwts.parserBuilder().build().parseClaimsJwt(untrustedJwtString).getBody();
      } catch (ClaimJwtException e) {
        // ignoring expiry exceptions
        untrustedClaims = e.getClaims();
      }
      for (Map.Entry<String, Object> entry : untrustedClaims.entrySet()) {
        switch (entry.getKey()) {
          case "aud":
            Object aud = entry.getValue();
            if (aud instanceof String) {
              withAudience((String) aud);
            } else if (aud instanceof Collection) {
              withAudiences((Collection<String>) aud);
            }
            break;
          case "azp":
            withAuthorizedParty(getTypedValue(entry, String.class));
            break;
          case "sub":
            withSubject(getTypedValue(entry, String.class));
            break;
          case "name":
            withName(getTypedValue(entry, String.class));
            break;
          case "given_name":
            withGivenName(getTypedValue(entry, String.class));
            break;
          case "family_name":
            withFamilyName(getTypedValue(entry, String.class));
            break;
          case "email":
            withEmail(getTypedValue(entry, String.class));
            break;
          case "preferred_username":
            withPreferredUsername(getTypedValue(entry, String.class));
            break;
          case "realm_access":
            Map<String, List<String>> sourceRealmAccess = getTypedValue(entry, Map.class);
            withRealmRoles(sourceRealmAccess.get("roles"));
            break;
          case "resource_access":
            Map<String, Map<String, List<String>>> sourceResourceAccess =
                getTypedValue(entry, Map.class);
            sourceResourceAccess.forEach(
                (key, value) -> withResourceRoles(key, value.get("roles")));
            break;
          case "scope":
            withScopes(Arrays.asList(getTypedValue(entry, String.class).split(" ")));
            break;
          case "acr":
            withAuthenticationContextClassReference(getTypedValue(entry, String.class));
            break;
          case "typ":
            if (!"Bearer".equals(getTypedValue(entry, String.class))) {
              throw new IllegalArgumentException("Only bearer tokens are allowed here!");
            }
            break;
          case "iss":
            String issuer = getTypedValue(entry, String.class);
            try {
              URI issuerUrl = new URI(issuer);
              withHostname(issuerUrl.getHost());
              withRealm(getRealm(issuerUrl));
            } catch (URISyntaxException e) {
              throw new IllegalArgumentException("Issuer '" + issuer + "' is not a valid URL", e);
            }
            break;
          case "iat":
          case "nbf":
          case "exp":
          case "auth_time":
            // ignoring date information
            break;
          default:
            withClaim(entry.getKey(), entry.getValue());
            break;
        }
      }
      return this;
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    private <T> T getTypedValue(
        @Nonnull final Map.Entry<String, Object> entry, @Nonnull final Class<T> clazz) {
      if (clazz.isInstance(entry.getValue())) {
        return (T) entry.getValue();
      }
      throw new IllegalArgumentException(
          String.format(
              "Expected %s for key %s, but found %s",
              clazz, entry.getKey(), entry.getValue().getClass()));
    }

    @Nonnull
    private String getRealm(@Nonnull final URI issuer) {
      Matcher matcher = ISSUER_PATH_PATTERN.matcher(issuer.getPath());
      if (!matcher.matches()) {
        throw new IllegalArgumentException(
            "The issuer '"
                + issuer
                + "' did not conform to the expected format"
                + " 'http[s]://$HOSTNAME[:port]/auth/realms/$REALM'.");
      }
      return matcher.group(1);
    }

    /**
     * Add an audience.
     *
     * <p>An audience is an identifier of a recipient of the token.
     *
     * @param audience the audience to add
     * @return builder
     * @see <a href="https://openid.net/specs/openid-connect-core-1_0.html#IDToken">ID token</a>
     */
    @Nonnull
    public Builder withAudience(@Nonnull final String audience) {
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
     * @see <a href="https://openid.net/specs/openid-connect-core-1_0.html#IDToken">ID token</a>
     */
    @Nonnull
    public Builder withAudiences(@Nonnull final Collection<String> audiences) {
      this.audience.addAll(Objects.requireNonNull(audiences));
      return this;
    }

    /**
     * Set authorized party.
     *
     * <p>The authorized party identifies the party for which the token was issued.
     *
     * @param authorizedParty the authorized party to set
     * @return builder
     * @see <a href="https://openid.net/specs/openid-connect-core-1_0.html#IDToken">ID token</a>
     */
    @Nonnull
    public Builder withAuthorizedParty(@Nonnull final String authorizedParty) {
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
     * @see <a href="https://openid.net/specs/openid-connect-core-1_0.html#IDToken">ID token</a>
     */
    @Nonnull
    public Builder withSubject(@Nonnull final String subject) {
      this.subject = Objects.requireNonNull(subject);
      return this;
    }

    /**
     * Add scope.
     *
     * <p>The scope for which this token has been requested. Always contains the scopes configured
     * in ServerConfig.
     *
     * @param scope the scope to add
     * @return builder
     * @see <a href="https://openid.net/specs/openid-connect-core-1_0.html#ScopeClaims">scope
     *     claims</a>
     */
    @Nonnull
    public Builder withScope(@Nonnull final String scope) {
      this.scope.add(scope);
      return this;
    }

    /**
     * Add scopes.
     *
     * <p>The scopes for which this token has been requested. Always contains the scopes configured
     * in ServerConfig.
     *
     * @param scopes the scopes to add
     * @return builder
     * @see <a href="https://openid.net/specs/openid-connect-core-1_0.html#ScopeClaims">scope
     *     claims</a>
     */
    @Nonnull
    public Builder withScopes(@Nonnull final Collection<String> scopes) {
      this.scope.addAll(scopes);
      return this;
    }

    /**
     * Add authorization hostname.
     *
     * <p>The hostname for which this token has been requested. If not set, the default hostname of
     * the mock is used.
     *
     * <p>This will be used to construct the issuer "iss": http[s]://$HOSTNAME/auth/realms/$REALM.
     * The protocol is taken from the server configuration.
     *
     * <p>Note: The hostname must not contain a protocol prefix like 'http://'.
     *
     * @param hostname the hostname
     * @return builder
     * @see #withRealm(String)
     * @see ServerConfig#getProtocol()
     */
    @Nonnull
    public Builder withHostname(@Nonnull final String hostname) {
      this.hostname = Objects.requireNonNull(hostname);
      return this;
    }

    /**
     * Add authorization realm.
     *
     * <p>The realm for which this token has been requested. If not set, the default realm of the
     * mock is used.
     *
     * <p>This will be used to construct the issuer "iss": http[s]://$HOSTNAME/auth/realms/$REALM.
     * The protocol is taken from the server configuration.
     *
     * @param realm the realm
     * @return builder
     * @see #withHostname(String)
     * @see ServerConfig#getProtocol()
     */
    @Nonnull
    public Builder withRealm(@Nonnull final String realm) {
      this.realm = Objects.requireNonNull(realm);
      return this;
    }

    /**
     * Add realm roles.
     *
     * <p>Realm roles apply to all clients within a realm.
     *
     * @param roles the roles to add
     * @return builder
     * @see <a href="https://www.keycloak.org/docs/latest/server_admin/index.html#realm-roles">realm
     *     roles</a>
     */
    @Nonnull
    public Builder withRealmRoles(@Nonnull final Collection<String> roles) {
      this.realmRoles.addRoles(Objects.requireNonNull(roles));
      return this;
    }

    /**
     * Add a realm role.
     *
     * <p>Realm roles apply to all clients within a realm.
     *
     * @param role the role to add
     * @return builder
     * @see <a href="https://www.keycloak.org/docs/latest/server_admin/index.html#realm-roles">realm
     *     roles</a>
     */
    @Nonnull
    public Builder withRealmRole(@Nonnull final String role) {
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
     * @see <a
     *     href="https://www.keycloak.org/docs/latest/server_admin/index.html#client-roles">client
     *     roles</a>
     */
    @Nonnull
    public Builder withResourceRoles(
        @Nonnull final String resource, @Nonnull final Collection<String> roles) {
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
     * @see <a
     *     href="https://www.keycloak.org/docs/latest/server_admin/index.html#client-roles">client
     *     roles</a>
     */
    @Nonnull
    public Builder withResourceRole(@Nonnull final String resource, @Nonnull final String role) {
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
    public Builder withClaims(@Nonnull final Map<String, Object> claims) {
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
    public Builder withClaim(@Nonnull final String key, @Nonnull final Object value) {
      this.claims.put(Objects.requireNonNull(key), Objects.requireNonNull(value));
      return this;
    }

    /**
     * Set issued at date.
     *
     * @param issuedAt the instant when the token was generated
     * @return builder
     * @see <a href="https://openid.net/specs/openid-connect-core-1_0.html#IDToken">ID token</a>
     */
    @Nonnull
    public Builder withIssuedAt(@Nonnull final Instant issuedAt) {
      this.issuedAt = Objects.requireNonNull(issuedAt);
      return this;
    }

    /**
     * Set authentication time.
     *
     * @param authenticationTime the instant when user was authenticated at the SSO server
     * @return builder
     * @see <a href="https://openid.net/specs/openid-connect-core-1_0.html#IDToken">ID token</a>
     */
    @Nonnull
    public Builder withAuthenticationTime(@Nonnull final Instant authenticationTime) {
      this.authenticationTime = Objects.requireNonNull(authenticationTime);
      return this;
    }

    /**
     * Set expiration date.
     *
     * @param expiration the instant when the token expires
     * @return builder
     * @see <a href="https://openid.net/specs/openid-connect-core-1_0.html#IDToken">ID token</a>
     */
    @Nonnull
    public Builder withExpiration(@Nonnull final Instant expiration) {
      this.expiration = Objects.requireNonNull(expiration);
      return this;
    }

    /**
     * Set not before date.
     *
     * @param notBefore the instant when the token starts being valid
     * @return builder
     * @see <a href="https://tools.ietf.org/html/rfc7519#section-4.1.5">Not Before Claim</a>
     */
    @Nonnull
    public Builder withNotBefore(@Nullable final Instant notBefore) {
      this.notBefore = notBefore;
      return this;
    }

    /**
     * Set given name.
     *
     * @param givenName the given name of the user
     * @return builder
     * @see <a
     *     href="https://www.keycloak.org/docs/latest/server_admin/index.html#_create-new-user">create
     *     new user</a>
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
     * @see <a
     *     href="https://www.keycloak.org/docs/latest/server_admin/index.html#_create-new-user">create
     *     new user</a>
     */
    @Nonnull
    public Builder withFamilyName(@Nullable final String familyName) {
      this.familyName = familyName;
      return this;
    }

    /**
     * Set full name.
     *
     * <p>If not set, this will automatically be filled using given name and family name.
     *
     * @param name the full name of the user
     * @return builder
     */
    @Nonnull
    public Builder withName(@Nullable final String name) {
      this.name = name;
      return this;
    }

    /**
     * Set email address.
     *
     * @param email the email address of the user
     * @return builder
     * @see <a
     *     href="https://www.keycloak.org/docs/latest/server_admin/index.html#_create-new-user">create
     *     new user</a>
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
     * @see <a
     *     href="https://www.keycloak.org/docs/latest/server_admin/index.html#_create-new-user">create
     *     new user</a>
     */
    @Nonnull
    public Builder withPreferredUsername(@Nullable final String preferredUsername) {
      this.preferredUsername = preferredUsername;
      return this;
    }

    /**
     * Set authentication context class reference.
     *
     * @param authenticationContextClassReference the reference class of the authentication ("0" or
     *     "1")
     * @return builder
     * @see <a href="https://openid.net/specs/openid-connect-core-1_0.html#IDToken">ID token</a>
     */
    @Nonnull
    public Builder withAuthenticationContextClassReference(
        @Nullable final String authenticationContextClassReference) {
      this.authenticationContextClassReference = authenticationContextClassReference;
      return this;
    }

    @Nonnull
    public TokenConfig build() {
      return new TokenConfig(this);
    }
  }

  /**
   * A container for realm or resource roles, to be used in claims {@code realm_access} or {@code
   * resource_access}.
   *
   * @see Builder#withRealmRole(String)
   * @see Builder#withResourceRole(String, String)
   */
  public static class Access {

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
