package com.tngtech.keycloakmock.impl.session;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class AdHocSession implements Session {
  @Nonnull private final UserData userData;
  @Nonnull private final List<String> roles;
  @Nonnull private final String clientId;
  @Nonnull private final String sessionId = UUID.randomUUID().toString();

  private AdHocSession(
      @Nonnull UserData userData, @Nonnull List<String> roles, @Nonnull String clientId) {
    this.userData = userData;
    this.roles = roles;
    this.clientId = clientId;
  }

  public static AdHocSession fromClientIdUsernameAndPassword(
      @Nonnull String clientId,
      @Nonnull String hostname,
      @Nonnull String username,
      @Nullable String password) {
    List<String> roles =
        Optional.ofNullable(password)
            .map(s -> Arrays.asList(s.split(",")))
            .orElseGet(Collections::emptyList);
    return new AdHocSession(UserData.fromUsernameAndHostname(username, hostname), roles, clientId);
  }

  @Nonnull
  @Override
  public UserData getUserData() {
    return userData;
  }

  @Nonnull
  @Override
  public List<String> getRoles() {
    return roles;
  }

  @Nonnull
  @Override
  public String getClientId() {
    return clientId;
  }

  @Nonnull
  @Override
  public String getSessionId() {
    return sessionId;
  }

  @Nullable
  @Override
  public String getNonce() {
    // always null
    return null;
  }
}
