package com.tngtech.keycloakmock.impl.session;

import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class Session {

  @Nullable private String username;
  private List<String> roles = Collections.emptyList();
  private String clientId;
  private String state;
  private String nonce;
  private String redirectUri;
  private String sessionId;
  private String responseType;
  private String responseMode;

  @Nullable
  public String getUsername() {
    return username;
  }

  public Session setUsername(@Nullable String username) {
    this.username = username;
    return this;
  }

  @Nonnull
  public List<String> getRoles() {
    return roles;
  }

  public Session setRoles(@Nonnull List<String> roles) {
    this.roles = roles;
    return this;
  }

  @Nonnull
  public String getClientId() {
    return clientId;
  }

  public Session setClientId(@Nonnull String clientId) {
    this.clientId = clientId;
    return this;
  }

  @Nonnull
  public String getState() {
    return state;
  }

  public Session setState(@Nonnull String state) {
    this.state = state;
    return this;
  }

  @Nonnull
  public String getNonce() {
    return nonce;
  }

  public Session setNonce(@Nonnull String nonce) {
    this.nonce = nonce;
    return this;
  }

  @Nonnull
  public String getRedirectUri() {
    return redirectUri;
  }

  public Session setRedirectUri(@Nonnull String redirectUri) {
    this.redirectUri = redirectUri;
    return this;
  }

  @Nonnull
  public String getSessionId() {
    return sessionId;
  }

  public Session setSessionId(@Nonnull String sessionId) {
    this.sessionId = sessionId;
    return this;
  }

  @Nonnull
  public String getResponseType() {
    return responseType;
  }

  public Session setResponseType(@Nonnull String responseType) {
    this.responseType = responseType;
    return this;
  }

  @Nonnull
  public String getResponseMode() {
    return responseMode;
  }

  public Session setResponseMode(@Nonnull String responseMode) {
    this.responseMode = responseMode;
    return this;
  }
}
