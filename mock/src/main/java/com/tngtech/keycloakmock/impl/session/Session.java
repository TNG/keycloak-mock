package com.tngtech.keycloakmock.impl.session;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface Session {
  @Nonnull
  String getClientId();

  @Nonnull
  String getSessionId();

  @Nonnull
  UserData getUserData();

  @Nonnull
  List<String> getRoles();

  @Nullable
  String getNonce();
}
