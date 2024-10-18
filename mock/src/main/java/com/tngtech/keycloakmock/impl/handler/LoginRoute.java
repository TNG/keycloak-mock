package com.tngtech.keycloakmock.impl.handler;

import static com.tngtech.keycloakmock.impl.helper.RedirectHelper.KEYCLOAK_SESSION_COOKIE;

import com.tngtech.keycloakmock.impl.UrlConfiguration;
import com.tngtech.keycloakmock.impl.helper.RedirectHelper;
import com.tngtech.keycloakmock.impl.session.PersistentSession;
import com.tngtech.keycloakmock.impl.session.SessionRepository;
import com.tngtech.keycloakmock.impl.session.SessionRequest;
import io.vertx.core.Handler;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.common.template.TemplateEngine;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
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
  @Nonnull private final UrlConfiguration baseConfiguration;

  @Inject
  LoginRoute(
      @Nonnull SessionRepository sessionRepository,
      @Nonnull RedirectHelper redirectHelper,
      @Nonnull TemplateEngine engine,
      @Nonnull UrlConfiguration baseConfiguration) {
    this.sessionRepository = sessionRepository;
    this.redirectHelper = redirectHelper;
    this.engine = engine;
    this.baseConfiguration = baseConfiguration;
  }

  @Override
  public void handle(@Nonnull RoutingContext routingContext) {
    // if we have a stored session with a valid token, re-use it
    Optional<PersistentSession> existingSession =
        Optional.ofNullable(routingContext.request().getCookie(KEYCLOAK_SESSION_COOKIE))
            .map(Cookie::getValue)
            .map(value -> value.split("/"))
            .filter(split -> split.length > 0)
            .map(split -> split[split.length - 1])
            .map(sessionRepository::getSession);

    // for now, we just override the settings of the session with values of the new client
    SessionRequest request =
        new SessionRequest.Builder()
            .setClientId(routingContext.queryParams().get(CLIENT_ID))
            .setState(routingContext.queryParams().get(STATE))
            .setRedirectUri(routingContext.queryParams().get(REDIRECT_URI))
            .setSessionId(
                existingSession
                    .map(PersistentSession::getSessionId)
                    .orElseGet(() -> UUID.randomUUID().toString()))
            .setResponseType(routingContext.queryParams().get(RESPONSE_TYPE))
            // optional parameter
            .setNonce(routingContext.queryParams().get(NONCE))
            .setResponseMode(routingContext.queryParams().get(RESPONSE_MODE))
            .build();

    UrlConfiguration requestConfiguration = baseConfiguration.forRequestContext(routingContext);
    if (existingSession.isPresent()) {
      PersistentSession oldSession = existingSession.get();
      PersistentSession newSession =
          request.toSession(oldSession.getUserData(), oldSession.getRoles());
      sessionRepository.updateSession(oldSession, newSession);
      routingContext
          .response()
          .addCookie(redirectHelper.getSessionCookie(newSession, requestConfiguration))
          .putHeader(
              "location", redirectHelper.getRedirectLocation(newSession, requestConfiguration))
          .setStatusCode(302)
          .end();
    } else {
      sessionRepository.putRequest(request);
      routingContext.put(
          AUTHENTICATION_URI,
          requestConfiguration.getAuthenticationCallbackEndpoint(request.getSessionId()));
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
