package com.tngtech.keycloakmock.standalone.session;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SessionRepository {

  @Nonnull private final Map<String, Session> sessions = new HashMap<>();

  @Nullable
  public Session getSession(@Nonnull final String sessionId) {
    return sessions.get(sessionId);
  }

  public void putSession(@Nonnull final Session session) {
    sessions.put(session.getSessionId(), session);
  }

  public void removeSession(@Nonnull final String sessionId) {
    sessions.remove(sessionId);
  }
}
