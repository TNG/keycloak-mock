package com.tngtech.keycloakmock.standalone.handler;

import static com.tngtech.keycloakmock.api.KeycloakMock.CTX_HOSTNAME;
import static com.tngtech.keycloakmock.api.TokenConfig.aTokenConfig;

import com.tngtech.keycloakmock.standalone.token.TokenFactory;
import com.tngtech.keycloakmock.standalone.token.TokenRepository;
import io.vertx.core.Handler;
import io.vertx.core.http.Cookie;
import io.vertx.ext.web.RoutingContext;
import java.time.Instant;
import java.util.Arrays;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthenticationRoute implements Handler<RoutingContext> {
  private static final Logger LOG = LoggerFactory.getLogger(AuthenticationRoute.class);
  private static final String CLIENT_ID = "client_id";
  private static final String STATE = "state";
  private static final String NONCE = "nonce";
  private static final String REDIRECT_URI = "redirect_uri";
  private static final String REALM = "realm";
  private static final String SESSION_ID = "session_id";
  private static final String RESPONSE_TYPE = "response_type";
  private static final String RESPONSE_MODE = "response_mode";
  private static final String SESSION_STATE = "session_state";
  private static final String USER = "user";
  private static final String ROLES = "roles";
  private static final String CODE = "code";
  private static final String ID_TOKEN = "id_token";
  private static final String ACCESS_TOKEN = "access_token";
  private static final String TOKEN_TYPE = "token_type";

  @Nonnull private final TokenFactory tokenFactory;
  @Nonnull private final TokenRepository tokenRepository;

  public AuthenticationRoute(
      @Nonnull final TokenFactory tokenFactory, @Nonnull final TokenRepository tokenRepository) {
    this.tokenFactory = tokenFactory;
    this.tokenRepository = tokenRepository;
  }

  @Override
  public void handle(@Nonnull final RoutingContext routingContext) {
    ResponseType responseType =
        ResponseType.fromValueOrNull(routingContext.queryParams().get(RESPONSE_TYPE));
    if (responseType == null) {
      LOG.warn(
          "Invalid response type '{}' requested!", routingContext.queryParams().get(RESPONSE_TYPE));
      routingContext.fail(400);
      return;
    }
    String realm = routingContext.queryParams().get(REALM);
    String sessionId = routingContext.queryParams().get(SESSION_ID);
    String requestHostname = routingContext.get(CTX_HOSTNAME);
    // for simplicity, the access token is the same as the ID token
    String token =
        tokenFactory.getToken(
            aTokenConfig()
                .withAudience(routingContext.queryParams().get(CLIENT_ID))
                .withIssuedAt(Instant.now())
                .withSubject(routingContext.queryParams().get(USER))
                .withRealmRoles(
                    Arrays.asList(routingContext.queryParams().get(ROLES).trim().split(",")))
                .withClaim(NONCE, routingContext.queryParams().get(NONCE))
                .withClaim(SESSION_STATE, sessionId)
                .build(),
            requestHostname,
            realm);
    ResponseMode responseMode =
        responseType.getValidResponseMode(routingContext.queryParams().get(RESPONSE_MODE));
    StringBuilder redirectUri = new StringBuilder(routingContext.queryParams().get(REDIRECT_URI));
    redirectUri.append(
        getResponseParameter(responseMode, STATE, routingContext.queryParams().get(STATE)));
    redirectUri.append(
        getResponseParameter(null, SESSION_STATE, routingContext.queryParams().get(STATE)));
    switch (responseType) {
      case CODE:
        // for simplicity, use session ID as authorization code
        tokenRepository.putToken(sessionId, token);
        redirectUri.append(getResponseParameter(null, CODE, sessionId));
        break;
      case ID_TOKEN:
        redirectUri.append(getResponseParameter(null, ID_TOKEN, token));
        break;
      case ID_TOKEN_PLUS_TOKEN:
        redirectUri.append(getResponseParameter(null, ID_TOKEN, token));
        redirectUri.append(getResponseParameter(null, ACCESS_TOKEN, token));
        redirectUri.append(getResponseParameter(null, TOKEN_TYPE, "bearer"));
        break;
      case NONE:
      default:
        break;
    }
    routingContext
        .addCookie(
            Cookie.cookie("KEYCLOAK_SESSION", realm + "/no-idea-what-goes-here/" + sessionId)
                .setPath("/auth/realms/" + realm + "/")
                .setMaxAge(36000)
                .setSecure(false))
        .response()
        .putHeader("location", redirectUri.toString())
        .setStatusCode(302)
        .end();
  }

  private String getResponseParameter(
      @Nullable final ResponseMode responseMode,
      @Nonnull final String name,
      @Nonnull final String value) {
    return (responseMode != null ? responseMode.getSign() : "&") + name + "=" + value;
  }
}
