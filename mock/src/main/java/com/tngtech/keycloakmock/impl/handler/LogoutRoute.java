package com.tngtech.keycloakmock.impl.handler;

import static com.tngtech.keycloakmock.impl.handler.RequestUrlConfigurationHandler.CTX_REQUEST_CONFIGURATION;
import static com.tngtech.keycloakmock.impl.helper.RedirectHelper.KEYCLOAK_SESSION_COOKIE;

import com.tngtech.keycloakmock.impl.UrlConfiguration;
import com.tngtech.keycloakmock.impl.helper.RedirectHelper;
import com.tngtech.keycloakmock.impl.session.SessionRepository;
import io.vertx.core.Handler;
import io.vertx.core.http.Cookie;
import io.vertx.ext.web.RoutingContext;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @see <a
 *     href="https://github.com/keycloak/keycloak-documentation/blob/main/securing_apps/topics/oidc/java/logout.adoc">Logout</a>
 */
@Singleton
public class LogoutRoute implements Handler<RoutingContext> {

  /**
   * <a href="https://github.com/keycloak/keycloak/pull/10887/">Before Keycloak 18</a>, the logout
   * endpoint had used the {@value #LEGACY_REDIRECT_URI} query parameter.
   */
  private static final String LEGACY_REDIRECT_URI = "redirect_uri";

  private static final String POST_LOGOUT_REDIRECT_URI = "post_logout_redirect_uri";
  @Nonnull private final SessionRepository sessionRepository;
  @Nonnull private final RedirectHelper redirectHelper;

  @Inject
  LogoutRoute(
      @Nonnull SessionRepository sessionRepository, @Nonnull RedirectHelper redirectHelper) {
    this.sessionRepository = sessionRepository;
    this.redirectHelper = redirectHelper;
  }

  @Override
  public void handle(@Nonnull RoutingContext routingContext) {
    String redirectUri = routingContext.queryParams().get(POST_LOGOUT_REDIRECT_URI);
    if (redirectUri == null) { // for backwards compatibility:
      redirectUri = routingContext.queryParams().get(LEGACY_REDIRECT_URI);
    }
    UrlConfiguration requestConfiguration = routingContext.get(CTX_REQUEST_CONFIGURATION);
    invalidateSession(routingContext);
    routingContext
        .addCookie(redirectHelper.invalidateSessionCookie(requestConfiguration))
        .response()
        .putHeader("location", redirectUri)
        .setStatusCode(302)
        .end();
  }

  private void invalidateSession(RoutingContext routingContext) {
    Optional.ofNullable(routingContext.getCookie(KEYCLOAK_SESSION_COOKIE))
        .map(Cookie::getValue)
        .map(s -> s.split("/"))
        .filter(s -> s.length > 0)
        .map(s -> s[s.length - 1])
        .ifPresent(sessionRepository::removeSession);
  }
}
