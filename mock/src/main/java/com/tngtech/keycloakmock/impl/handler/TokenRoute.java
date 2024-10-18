package com.tngtech.keycloakmock.impl.handler;

import com.tngtech.keycloakmock.impl.UrlConfiguration;
import com.tngtech.keycloakmock.impl.helper.TokenHelper;
import com.tngtech.keycloakmock.impl.session.AdHocSession;
import com.tngtech.keycloakmock.impl.session.Session;
import com.tngtech.keycloakmock.impl.session.SessionRepository;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TokenRoute implements Handler<RoutingContext> {

  private static final String GRANT_TYPE = "grant_type";
  private static final String CODE = "code";
  private static final String SESSION_STATE = "session_state";

  @Nonnull private final SessionRepository sessionRepository;
  @Nonnull private final TokenHelper tokenHelper;
  @Nonnull private final UrlConfiguration baseConfiguration;

  @Inject
  TokenRoute(
      @Nonnull SessionRepository sessionRepository,
      @Nonnull TokenHelper tokenHelper,
      @Nonnull UrlConfiguration baseConfiguration) {
    this.sessionRepository = sessionRepository;
    this.tokenHelper = tokenHelper;
    this.baseConfiguration = baseConfiguration;
  }

  @Override
  public void handle(@Nonnull RoutingContext routingContext) {
    String grantType = routingContext.request().getFormAttribute(GRANT_TYPE);
    switch (Objects.toString(grantType)) {
      case "authorization_code":
        handleAuthorizationCodeFlow(routingContext);
        break;
      case "refresh_token":
        handleRefreshTokenFlow(routingContext);
        break;
      case "password":
        handlePasswordFlow(routingContext);
        break;
      case "client_credentials":
        handleClientCredentialsFlow(routingContext);
        break;
      default:
        routingContext.fail(400);
    }
  }

  private void handleAuthorizationCodeFlow(RoutingContext routingContext) {
    // here again we use the equality of authorization code and session ID
    String sessionId = routingContext.request().getFormAttribute(CODE);
    UrlConfiguration requestConfiguration = baseConfiguration.forRequestContext(routingContext);
    String token =
        Optional.ofNullable(sessionRepository.getSession(sessionId))
            .map(s -> tokenHelper.getToken(s, requestConfiguration))
            .orElse(null);
    if (token == null) {
      routingContext.fail(404);
      return;
    }
    routingContext
        .response()
        .putHeader("content-type", "application/json")
        .end(toTokenResponse(token, sessionId));
  }

  private void handleRefreshTokenFlow(RoutingContext routingContext) {
    String refreshToken = routingContext.request().getFormAttribute("refresh_token");
    if (refreshToken == null || refreshToken.isEmpty()) {
      routingContext.fail(400);
      return;
    }
    Map<String, Object> token = tokenHelper.parseToken(refreshToken);
    String sessionId = (String) token.get(SESSION_STATE);

    routingContext
        .response()
        .putHeader("content-type", "application/json")
        .end(toTokenResponse(refreshToken, sessionId));
  }

  private void handlePasswordFlow(RoutingContext routingContext) {
    String clientId = routingContext.request().getFormAttribute("client_id");
    if (clientId == null || clientId.isEmpty()) {
      User user = routingContext.user();
      if (user != null) {
        clientId = user.get("username");
      }
    }
    if (clientId == null || clientId.isEmpty()) {
      routingContext.fail(400);
      return;
    }
    String username = routingContext.request().getFormAttribute("username");
    if (username == null || username.isEmpty()) {
      routingContext.fail(400);
      return;
    }
    UrlConfiguration requestConfiguration = baseConfiguration.forRequestContext(routingContext);
    String password = routingContext.request().getFormAttribute("password");

    Session session =
        AdHocSession.fromClientIdUsernameAndPassword(
            clientId, requestConfiguration.getHostname(), username, password);
    String token = tokenHelper.getToken(session, requestConfiguration);

    routingContext
        .response()
        .putHeader("content-type", "application/json")
        .end(toTokenResponse(token, session.getSessionId()));
  }

  private void handleClientCredentialsFlow(RoutingContext routingContext) {
    String clientId = routingContext.request().getFormAttribute("client_id");
    String password = routingContext.request().getFormAttribute("client_secret");
    boolean formBasedAuth =
        clientId != null && !clientId.isEmpty() && password != null && !password.isEmpty();
    final User user = routingContext.user();
    if (user == null && !formBasedAuth) {
      routingContext.fail(401);
      return;
    }

    // if not form based, try using user (BASIC auth or custom)
    if (!formBasedAuth) {
      clientId = routingContext.user().get("username");
      // Password is a list of roles
      password = routingContext.user().get("password");
    }

    if (clientId == null || clientId.isEmpty()) {
      routingContext.fail(400);
      return;
    }

    final UrlConfiguration requestConfiguration =
        baseConfiguration.forRequestContext(routingContext);

    final Session session =
        AdHocSession.fromClientIdUsernameAndPassword(
            clientId, requestConfiguration.getHostname(), clientId, password);

    final String token = tokenHelper.getToken(session, requestConfiguration);

    routingContext
        .response()
        .putHeader("content-type", "application/json")
        .end(toTokenResponse(token, session.getSessionId()));
  }

  private String toTokenResponse(String token, String sessionId) {
    return new JsonObject()
        .put("access_token", token)
        .put("token_type", "Bearer")
        .put("expires_in", 36_000)
        .put("refresh_token", token)
        .put("refresh_expires_in", 36_000)
        .put("id_token", token)
        .put(SESSION_STATE, sessionId)
        .encode();
  }
}
