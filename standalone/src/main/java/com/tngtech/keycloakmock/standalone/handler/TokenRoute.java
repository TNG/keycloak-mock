package com.tngtech.keycloakmock.standalone.handler;

import com.tngtech.keycloakmock.standalone.render.RenderHelper;
import com.tngtech.keycloakmock.standalone.token.TokenRepository;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class TokenRoute implements Handler<RoutingContext> {

  private static final String GRANT_TYPE = "grant_type";
  private static final String CODE = "code";
  private static final String SESSION_STATE = "session_state";

  private final TokenRepository tokenRepository;
  private final RenderHelper renderHelper;

  public TokenRoute(TokenRepository tokenRepository, RenderHelper renderHelper) {
    this.tokenRepository = tokenRepository;
    this.renderHelper = renderHelper;
  }

  @Override
  public void handle(RoutingContext routingContext) {
    if (!"authorization_code".equals(routingContext.request().getFormAttribute(GRANT_TYPE))) {
      routingContext.fail(400);
      return;
    }
    // here again we use the equality of authorization code and session ID
    String authorizationCode = routingContext.request().getFormAttribute(CODE);
    String token = tokenRepository.getToken(authorizationCode);
    if (token == null) {
      routingContext.fail(404);
      return;
    }
    routingContext.put("token", token);
    routingContext.put(SESSION_STATE, authorizationCode);
    renderHelper.renderTemplate(routingContext, "tokenResponse.ftl", "application/json");
  }
}
