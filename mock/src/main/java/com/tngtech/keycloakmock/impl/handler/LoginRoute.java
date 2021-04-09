package com.tngtech.keycloakmock.impl.handler;

import static com.tngtech.keycloakmock.impl.handler.RequestUrlConfigurationHandler.CTX_REQUEST_CONFIGURATION;
import static com.tngtech.keycloakmock.impl.helper.RedirectHelper.KEYCLOAK_SESSION_COOKIE;

import com.tngtech.keycloakmock.impl.UrlConfiguration;
import com.tngtech.keycloakmock.impl.helper.RedirectHelper;
import com.tngtech.keycloakmock.impl.session.Session;
import com.tngtech.keycloakmock.impl.session.SessionRepository;
import io.vertx.core.Handler;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.common.template.TemplateEngine;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoginRoute implements Handler<RoutingContext> {

  private static final Logger LOG = LoggerFactory.getLogger(LoginRoute.class);
  private static final String AUTHENTICATION_URI = "authentication_uri";
  private static final String CLIENT_ID = "client_id";
  private static final String STATE = "state";
  private static final String NONCE = "nonce";
  private static final String REDIRECT_URI = "redirect_uri";
  private static final String RESPONSE_TYPE = "response_type";
  private static final String RESPONSE_MODE = "response_mode";
  @Nonnull private final SessionRepository sessionRepository;
  @Nonnull private final RedirectHelper redirectHelper;
  @Nonnull private final TemplateEngine engine;

  public LoginRoute(
      @Nonnull SessionRepository sessionRepository,
      @Nonnull RedirectHelper redirectHelper,
      @Nonnull TemplateEngine engine) {
    this.sessionRepository = sessionRepository;
    this.redirectHelper = redirectHelper;
    this.engine = engine;
  }

  @Override
  public void handle(@Nonnull final RoutingContext routingContext) {
    // if we have a stored session with a valid token, re-use it
    Optional<Session> existingSession =
        Optional.ofNullable(routingContext.getCookie(KEYCLOAK_SESSION_COOKIE))
            .map(Cookie::getValue)
            .map(value -> value.split("/"))
            .filter(split -> split.length > 0)
            .map(split -> split[split.length - 1])
            .map(sessionRepository::getSession)
            .filter(session -> session.getUsername() != null);

    // TODO: store sessions per client
    // for now, we just override the settings of the session with values of the new client
    Session session =
        new Session()
            .setUsername(existingSession.map(Session::getUsername).orElse(null))
            .setRoles(existingSession.map(Session::getRoles).orElseGet(Collections::emptyList))
            .setClientId(routingContext.queryParams().get(CLIENT_ID))
            .setState(routingContext.queryParams().get(STATE))
            .setNonce(routingContext.queryParams().get(NONCE))
            .setRedirectUri(routingContext.queryParams().get(REDIRECT_URI))
            .setSessionId(
                existingSession
                    .map(Session::getSessionId)
                    .orElseGet(() -> UUID.randomUUID().toString()))
            .setResponseType(routingContext.queryParams().get(RESPONSE_TYPE))
            // optional parameter
            .setResponseMode(routingContext.queryParams().get(RESPONSE_MODE));
    sessionRepository.putSession(session);

    UrlConfiguration requestConfiguration = routingContext.get(CTX_REQUEST_CONFIGURATION);
    if (existingSession.isPresent()) {
      routingContext
          .response()
          .addCookie(redirectHelper.getSessionCookie(session, requestConfiguration))
          .putHeader("location", redirectHelper.getRedirectLocation(session, requestConfiguration))
          .setStatusCode(302)
          .end();
    } else {
      routingContext.put(
          AUTHENTICATION_URI,
          requestConfiguration.getAuthenticationCallbackEndpoint(session.getSessionId()));
      engine
          .render(routingContext.data(), "loginPage.ftl")
          .onSuccess(
              b ->
                  routingContext.response().putHeader(HttpHeaders.CONTENT_TYPE, "text/html").end(b))
          .onFailure(
              t -> {
                LOG.error("Unable to render login page", t);
                routingContext.fail(t);
              });
    }
  }
}
