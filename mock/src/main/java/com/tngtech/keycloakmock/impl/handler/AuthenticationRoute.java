package com.tngtech.keycloakmock.impl.handler;

import static com.tngtech.keycloakmock.impl.handler.RequestUrlConfigurationHandler.CTX_REQUEST_CONFIGURATION;

import com.tngtech.keycloakmock.impl.UrlConfiguration;
import com.tngtech.keycloakmock.impl.helper.RedirectHelper;
import com.tngtech.keycloakmock.impl.helper.UserInputSanitizer;
import com.tngtech.keycloakmock.impl.session.PersistentSession;
import com.tngtech.keycloakmock.impl.session.SessionRepository;
import com.tngtech.keycloakmock.impl.session.SessionRequest;
import com.tngtech.keycloakmock.impl.session.UserData;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class AuthenticationRoute implements Handler<RoutingContext> {

  private static final Logger LOG = LoggerFactory.getLogger(AuthenticationRoute.class);
  private static final String USERNAME_PARAMETER = "username";
  private static final String ROLES_PARAMETER = "password";

  @Nonnull private final SessionRepository sessionRepository;
  @Nonnull private final RedirectHelper redirectHelper;

  @Inject
  AuthenticationRoute(
      @Nonnull SessionRepository sessionRepository, @Nonnull RedirectHelper redirectHelper) {
    this.sessionRepository = sessionRepository;
    this.redirectHelper = redirectHelper;
  }

  @Override
  public void handle(@Nonnull RoutingContext routingContext) {
    String sessionId = routingContext.pathParam("sessionId");
    SessionRequest request = sessionRepository.getRequest(sessionId);
    if (request == null) {
      LOG.warn("Login for unknown session {} requested!", new UserInputSanitizer(sessionId));
      routingContext.fail(404);
      return;
    }
    String username = routingContext.request().getFormAttribute(USERNAME_PARAMETER);
    if (username == null) {
      LOG.warn("Missing username {}", new UserInputSanitizer(username));
      routingContext.fail(400);
      return;
    }
    String rolesString = routingContext.request().getFormAttribute(ROLES_PARAMETER);
    List<String> roles =
        Optional.ofNullable(rolesString)
            .map(s -> Arrays.asList(s.split(",")))
            .orElseGet(Collections::emptyList);

    UrlConfiguration requestConfiguration = routingContext.get(CTX_REQUEST_CONFIGURATION);

    PersistentSession session =
        request.toSession(
            UserData.fromUsernameAndHostname(username, requestConfiguration.getHostname()), roles);
    sessionRepository.upgradeRequest(request, session);

    routingContext
        .response()
        .addCookie(redirectHelper.getSessionCookie(session, requestConfiguration))
        .putHeader("location", redirectHelper.getRedirectLocation(session, requestConfiguration))
        .setStatusCode(302)
        .end();
  }
}
