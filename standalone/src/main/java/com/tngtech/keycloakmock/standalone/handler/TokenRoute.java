package com.tngtech.keycloakmock.standalone.handler;

import static com.tngtech.keycloakmock.impl.handler.RequestUrlConfigurationHandler.CTX_REQUEST_CONFIGURATION;

import com.tngtech.keycloakmock.impl.UrlConfiguration;
import com.tngtech.keycloakmock.standalone.helper.RenderHelper;
import com.tngtech.keycloakmock.standalone.helper.TokenHelper;
import com.tngtech.keycloakmock.standalone.session.SessionRepository;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import java.util.Optional;
import javax.annotation.Nonnull;

public class TokenRoute implements Handler<RoutingContext> {

  private static final String GRANT_TYPE = "grant_type";
  private static final String CODE = "code";
  private static final String SESSION_STATE = "session_state";

  @Nonnull private final SessionRepository sessionRepository;
  @Nonnull private final TokenHelper tokenHelper;
  @Nonnull private final RenderHelper renderHelper;

  public TokenRoute(
      @Nonnull final SessionRepository sessionRepository,
      @Nonnull TokenHelper tokenHelper,
      @Nonnull final RenderHelper renderHelper) {
    this.sessionRepository = sessionRepository;
    this.tokenHelper = tokenHelper;
    this.renderHelper = renderHelper;
  }

  @Override
  public void handle(@Nonnull final RoutingContext routingContext) {
    if (!"authorization_code".equals(routingContext.request().getFormAttribute(GRANT_TYPE))) {
      routingContext.fail(400);
      return;
    }
    // here again we use the equality of authorization code and session ID
    String sessionId = routingContext.request().getFormAttribute(CODE);
    UrlConfiguration requestConfiguration = routingContext.get(CTX_REQUEST_CONFIGURATION);
    String token =
        Optional.ofNullable(sessionRepository.getSession(sessionId))
            .map(s -> tokenHelper.getToken(s, requestConfiguration))
            .orElse(null);
    if (token == null) {
      routingContext.fail(404);
      return;
    }
    routingContext.put("token", token);
    routingContext.put(SESSION_STATE, sessionId);
    renderHelper.renderTemplate(routingContext, "tokenResponse.ftl", "application/json");
  }
}
