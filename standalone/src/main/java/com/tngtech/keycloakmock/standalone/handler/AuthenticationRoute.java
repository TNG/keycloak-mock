package com.tngtech.keycloakmock.standalone.handler;

import static com.tngtech.keycloakmock.impl.handler.RequestUrlConfigurationHandler.CTX_REQUEST_CONFIGURATION;

import com.tngtech.keycloakmock.impl.UrlConfiguration;
import com.tngtech.keycloakmock.standalone.helper.RedirectHelper;
import com.tngtech.keycloakmock.standalone.session.Session;
import com.tngtech.keycloakmock.standalone.session.SessionRepository;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthenticationRoute implements Handler<RoutingContext> {

  private static final Logger LOG = LoggerFactory.getLogger(AuthenticationRoute.class);
  private static final String USERNAME_PARAMETER = "username";
  private static final String ROLES_PARAMETER = "password";

  @Nonnull private final SessionRepository sessionRepository;
  @Nonnull private final RedirectHelper redirectHelper;

  public AuthenticationRoute(
      @Nonnull final SessionRepository sessionRepository, @Nonnull RedirectHelper redirectHelper) {
    this.sessionRepository = sessionRepository;
    this.redirectHelper = redirectHelper;
  }

  @Override
  public void handle(@Nonnull final RoutingContext routingContext) {
    String sessionId = routingContext.pathParam("sessionId");
    Session session = sessionRepository.getSession(sessionId);
    if (session == null) {
      LOG.warn("Login for unknown session {} requested!", sessionId);
      routingContext.fail(404);
      return;
    }
    String username = routingContext.request().getFormAttribute(USERNAME_PARAMETER);
    String rolesString = routingContext.request().getFormAttribute(ROLES_PARAMETER);
    if (username == null || rolesString == null) {
      LOG.warn("Missing username {} or roles parameter {}", username, rolesString);
      routingContext.fail(400);
      return;
    }
    UrlConfiguration requestConfiguration = routingContext.get(CTX_REQUEST_CONFIGURATION);
    List<String> roles = Arrays.asList(rolesString.trim().split(","));
    session.setUsername(username);
    session.setRoles(roles);
    routingContext
        .response()
        .addCookie(redirectHelper.getSessionCookie(session, requestConfiguration))
        .putHeader("location", redirectHelper.getRedirectLocation(session, requestConfiguration))
        .setStatusCode(302)
        .end();
  }
}
