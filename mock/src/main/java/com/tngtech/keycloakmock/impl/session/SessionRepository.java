package com.tngtech.keycloakmock.impl.session;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SessionRepository {

  @Nonnull
  private final ConcurrentMap<String, RequestOrSession> sessions = new ConcurrentHashMap<>();

  @Inject
  SessionRepository() {}

  @Nullable
  public PersistentSession getSession(@Nonnull String sessionId) {
    return sessions.getOrDefault(sessionId, RequestOrSession.EMPTY).session;
  }

  public void updateSession(
      @Nonnull PersistentSession oldSession, @Nonnull PersistentSession newSession) {
    if (!sessions.replace(
        newSession.getSessionId(),
        new RequestOrSession(oldSession),
        new RequestOrSession(newSession))) {
      throw new InvalidSessionStateException(
          "Unable to re-use existing session, it was updated in the meantime. Session ID: "
              + newSession.getSessionId());
    }
  }

  public void upgradeRequest(
      @Nonnull SessionRequest existingRequest, @Nonnull PersistentSession newSession) {
    if (!sessions.replace(
        newSession.getSessionId(),
        new RequestOrSession(existingRequest),
        new RequestOrSession(newSession))) {
      throw new InvalidSessionStateException(
          "Unable to create session from request, it was updated in the meantime. Session ID: "
              + newSession.getSessionId());
    }
  }

  public void removeSession(@Nonnull String sessionId) {
    sessions.remove(sessionId);
  }

  @Nullable
  public SessionRequest getRequest(@Nonnull String sessionId) {
    return sessions.getOrDefault(sessionId, RequestOrSession.EMPTY).request;
  }

  public void putRequest(@Nonnull SessionRequest sequest) {
    if (sessions.putIfAbsent(sequest.getSessionId(), new RequestOrSession(sequest)) != null) {
      throw new InvalidSessionStateException(
          "Unable to create session request, session ID is already in use: "
              + sequest.getSessionId());
    }
  }

  private static class RequestOrSession {
    static final RequestOrSession EMPTY = new RequestOrSession();

    @Nullable private final SessionRequest request;
    @Nullable private final PersistentSession session;

    RequestOrSession(@Nonnull SessionRequest sessionRequest) {
      request = Objects.requireNonNull(sessionRequest);
      session = null;
    }

    RequestOrSession(@Nonnull PersistentSession persistentSession) {
      request = null;
      session = Objects.requireNonNull(persistentSession);
    }

    private RequestOrSession() {
      request = null;
      session = null;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      RequestOrSession requestOrSession = (RequestOrSession) o;
      return Objects.equals(request, requestOrSession.request)
          && Objects.equals(session, requestOrSession.session);
    }

    @Override
    public int hashCode() {
      return Objects.hash(request, session);
    }
  }
}
