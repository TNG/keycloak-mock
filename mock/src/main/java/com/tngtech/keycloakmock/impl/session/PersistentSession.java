package com.tngtech.keycloakmock.impl.session;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PersistentSession implements Session {

  @Nonnull private final String clientId;
  @Nonnull private final String sessionId;
  @Nonnull private final String username;
  @Nonnull private final List<String> roles;
  @Nullable private final String state;
  @Nonnull private final String redirectUri;
  @Nonnull private final String responseType;
  @Nullable private final String responseMode;
  @Nullable private final String nonce;

  PersistentSession(
      @Nonnull SessionRequest request, @Nonnull String username, @Nonnull List<String> roles) {
    this.clientId = request.getClientId();
    this.sessionId = request.getSessionId();
    this.username = username;
    this.roles = roles;
    this.state = request.getState();
    this.redirectUri = request.getRedirectUri();
    this.responseType = request.getResponseType();
    this.responseMode = request.getResponseMode();
    this.nonce = request.getNonce();
  }

  @Override
  @Nonnull
  public String getClientId() {
    return clientId;
  }

  @Override
  @Nonnull
  public String getSessionId() {
    return sessionId;
  }

  @Override
  @Nonnull
  public String getUsername() {
    return username;
  }

  @Override
  @Nonnull
  public List<String> getRoles() {
    return roles;
  }

  @Nullable
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

  @Override
  @Nullable
  public String getNonce() {
    return nonce;
  }
}
