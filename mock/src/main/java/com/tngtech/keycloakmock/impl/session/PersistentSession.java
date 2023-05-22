package com.tngtech.keycloakmock.impl.session;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PersistentSession implements Session {

  @Nonnull private final String clientId;
  @Nonnull private final String sessionId;
  @Nonnull private final UserData userData;
  @Nonnull private final List<String> roles;
  @Nullable private final String state;
  @Nonnull private final String redirectUri;
  @Nonnull private final String responseType;
  @Nullable private final String responseMode;
  @Nullable private final String nonce;

  PersistentSession(
      @Nonnull SessionRequest request, @Nonnull UserData userData, @Nonnull List<String> roles) {
    this.clientId = request.getClientId();
    this.sessionId = request.getSessionId();
    this.userData = userData;
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
  public UserData getUserData() {
    return userData;
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
