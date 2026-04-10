package com.tngtech.keycloakmock.impl.handler;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;

import com.tngtech.keycloakmock.impl.UrlConfiguration;
import com.tngtech.keycloakmock.impl.UrlConfigurationFactory;
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
  // token route parameters
  static final String TOKEN_PARAM_GRANT_TYPE = "grant_type";
  static final String TOKEN_PARAM_CODE = "code";
  static final String TOKEN_PARAM_REFRESH_TOKEN = "refresh_token";
  static final String TOKEN_PARAM_USERNAME = "username";
  static final String TOKEN_PARAM_PASSWORD = "password";
  // allowed grant types
  static final String GRANT_AUTHORIZATION_CODE = "authorization_code";
  static final String GRANT_REFRESH_TOKEN = "refresh_token";
  static final String GRANT_PASSWORD = "password";
  static final String GRANT_CLIENT_CREDENTIALS = "client_credentials";
  // token response fields
  static final String TOKEN_RESPONSE_ACCESS_TOKEN = "access_token";
  static final String TOKEN_RESPONSE_TOKEN_TYPE = "token_type";
  static final String TOKEN_RESPONSE_EXPIRES_IN = "expires_in";
  static final String TOKEN_RESPONSE_REFRESH_TOKEN = "refresh_token";
  static final String TOKEN_RESPONSE_REFRESH_EXPIRES_IN = "refresh_expires_in";
  static final String TOKEN_RESPONSE_ID_TOKEN = "id_token";
  static final String TOKEN_RESPONSE_SESSION_STATE = "session_state";

  @Nonnull private final SessionRepository sessionRepository;
  @Nonnull private final TokenHelper tokenHelper;
  @Nonnull private final UrlConfigurationFactory urlConfigurationFactory;

  @Inject
  TokenRoute(
      @Nonnull SessionRepository sessionRepository,
      @Nonnull TokenHelper tokenHelper,
      @Nonnull UrlConfigurationFactory urlConfigurationFactory) {
    this.sessionRepository = sessionRepository;
    this.tokenHelper = tokenHelper;
    this.urlConfigurationFactory = urlConfigurationFactory;
  }

  @Override
  public void handle(@Nonnull RoutingContext routingContext) {
    String grantType = routingContext.request().getFormAttribute(TOKEN_PARAM_GRANT_TYPE);
    switch (Objects.toString(grantType)) {
      case GRANT_AUTHORIZATION_CODE:
        handleAuthorizationCodeFlow(routingContext);
        break;
      case GRANT_REFRESH_TOKEN:
        handleRefreshTokenFlow(routingContext);
        break;
      case GRANT_PASSWORD:
        handlePasswordFlow(routingContext);
        break;
      case GRANT_CLIENT_CREDENTIALS:
        handleClientCredentialsFlow(routingContext);
        break;
      default:
        routingContext.fail(400);
    }
  }

  private void handleAuthorizationCodeFlow(RoutingContext routingContext) {
    // here again we use the equality of authorization code and session ID
    String sessionId = routingContext.request().getFormAttribute(TOKEN_PARAM_CODE);
    UrlConfiguration requestConfiguration = urlConfigurationFactory.create(routingContext);
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
        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .end(toTokenResponse(token, sessionId));
  }

  private void handleRefreshTokenFlow(RoutingContext routingContext) {
    String refreshToken = routingContext.request().getFormAttribute(TOKEN_PARAM_REFRESH_TOKEN);
    if (refreshToken == null || refreshToken.isEmpty()) {
      routingContext.fail(400);
      return;
    }
    Map<String, Object> token = tokenHelper.parseToken(refreshToken);
    String sessionId = (String) token.get(TOKEN_RESPONSE_SESSION_STATE);

    routingContext
        .response()
        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .end(toTokenResponse(refreshToken, sessionId));
  }

  private void handlePasswordFlow(RoutingContext routingContext) {
    String clientId =
        Optional.ofNullable(routingContext.user())
            .map(u -> u.<String>get(OptionalClientAuthHandler.CTX_CLIENT_ID))
            .orElse(null);
    if (clientId == null || clientId.isEmpty()) {
      routingContext.fail(400);
      return;
    }
    String username = routingContext.request().getFormAttribute(TOKEN_PARAM_USERNAME);
    if (username == null || username.isEmpty()) {
      routingContext.fail(400);
      return;
    }
    UrlConfiguration requestConfiguration = urlConfigurationFactory.create(routingContext);
    String password = routingContext.request().getFormAttribute(TOKEN_PARAM_PASSWORD);

    Session session =
        AdHocSession.fromClientIdUsernameAndPassword(
            clientId, requestConfiguration.getHostname(), username, password);
    String token = tokenHelper.getToken(session, requestConfiguration);

    routingContext
        .response()
        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .end(toTokenResponse(token, session.getSessionId()));
  }

  private void handleClientCredentialsFlow(RoutingContext routingContext) {
    User user = routingContext.user();
    if (user == null) {
      routingContext.fail(401);
      return;
    }

    String clientId = routingContext.user().get(OptionalClientAuthHandler.CTX_CLIENT_ID);
    String password = routingContext.user().get(OptionalClientAuthHandler.CTX_CLIENT_SECRET);

    if (clientId == null || clientId.isEmpty()) {
      routingContext.fail(400);
      return;
    }

    final UrlConfiguration requestConfiguration = urlConfigurationFactory.create(routingContext);

    final Session session =
        AdHocSession.fromClientIdUsernameAndPassword(
            clientId, requestConfiguration.getHostname(), clientId, password);

    final String token = tokenHelper.getToken(session, requestConfiguration);

    routingContext
        .response()
        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .end(toTokenResponse(token, session.getSessionId()));
  }

  private String toTokenResponse(String token, String sessionId) {
    return new JsonObject()
        .put(TOKEN_RESPONSE_ACCESS_TOKEN, token)
        .put(TOKEN_RESPONSE_TOKEN_TYPE, "Bearer")
        .put(TOKEN_RESPONSE_EXPIRES_IN, 36_000)
        .put(TOKEN_RESPONSE_REFRESH_TOKEN, token)
        .put(TOKEN_RESPONSE_REFRESH_EXPIRES_IN, 36_000)
        .put(TOKEN_RESPONSE_ID_TOKEN, token)
        .put(TOKEN_RESPONSE_SESSION_STATE, sessionId)
        .encode();
  }
}
