package com.tngtech.keycloakmock.impl.session;

import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SessionRequest {
  @Nonnull private final String clientId;
  @Nonnull private final String sessionId;
  @Nonnull private final String state;
  @Nonnull private final String redirectUri;
  @Nonnull private final String responseType;
  @Nullable private final String responseMode;
  @Nullable private final String nonce;

  private SessionRequest(Builder builder) {
    clientId = Objects.requireNonNull(builder.clientId);
    sessionId = Objects.requireNonNull(builder.sessionId);
    state = Objects.requireNonNull(builder.state);
    redirectUri = Objects.requireNonNull(builder.redirectUri);
    responseType = Objects.requireNonNull(builder.responseType);
    responseMode = builder.responseMode;
    nonce = builder.nonce;
  }

  @Nonnull
  public String getClientId() {
    return clientId;
  }

  @Nonnull
  public String getSessionId() {
    return sessionId;
  }

  @Nonnull
  public String getState() {
    return state;
  }

  @Nonnull
  public String getRedirectUri() {
    return redirectUri;
  }

  @Nonnull
  public String getResponseType() {
    return responseType;
  }

  @Nullable
  public String getResponseMode() {
    return responseMode;
  }

  @Nullable
  public String getNonce() {
    return nonce;
  }

  public PersistentSession toSession(@Nonnull String username, @Nonnull List<String> roles) {
    return new PersistentSession(this, username, roles);
  }

  public static class Builder {
    private String clientId;
    private String sessionId;
    private String state;
    private String redirectUri;
    private String responseType;
    @Nullable private String responseMode;
    @Nullable private String nonce;

    public Builder setClientId(@Nonnull String clientId) {
      this.clientId = Objects.requireNonNull(clientId);
      return this;
    }

    public Builder setState(@Nonnull String state) {
      this.state = Objects.requireNonNull(state);
      return this;
    }

    public Builder setRedirectUri(@Nonnull String redirectUri) {
      this.redirectUri = Objects.requireNonNull(redirectUri);
      return this;
    }

    public Builder setSessionId(@Nonnull String sessionId) {
      this.sessionId = Objects.requireNonNull(sessionId);
      return this;
    }

    public Builder setResponseType(@Nonnull String responseType) {
      this.responseType = Objects.requireNonNull(responseType);
      return this;
    }

    public Builder setResponseMode(@Nullable String responseMode) {
      this.responseMode = responseMode;
      return this;
    }

    public Builder setNonce(@Nullable String nonce) {
      this.nonce = nonce;
      return this;
    }

    public SessionRequest build() {
      return new SessionRequest(this);
    }
  }
}
