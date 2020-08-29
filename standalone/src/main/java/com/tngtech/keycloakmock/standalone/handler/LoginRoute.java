package com.tngtech.keycloakmock.standalone.handler;

import com.tngtech.keycloakmock.standalone.render.RenderHelper;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import java.util.UUID;
import javax.annotation.Nonnull;

public class LoginRoute implements Handler<RoutingContext> {
  private static final String CLIENT_ID = "client_id";
  private static final String STATE = "state";
  private static final String NONCE = "nonce";
  private static final String REDIRECT_URI = "redirect_uri";
  private static final String REALM = "realm";
  private static final String SESSION_ID = "session_id";
  private static final String RESPONSE_TYPE = "response_type";
  private static final String RESPONSE_MODE = "response_mode";
  @Nonnull private final RenderHelper renderHelper;

  public LoginRoute(@Nonnull RenderHelper renderHelper) {
    this.renderHelper = renderHelper;
  }

  @Override
  public void handle(@Nonnull final RoutingContext routingContext) {
    String sessionId = UUID.randomUUID().toString();
    routingContext.put(CLIENT_ID, routingContext.queryParams().get(CLIENT_ID));
    routingContext.put(STATE, routingContext.queryParams().get(STATE));
    routingContext.put(NONCE, routingContext.queryParams().get(NONCE));
    routingContext.put(REDIRECT_URI, routingContext.queryParams().get(REDIRECT_URI));
    String realm = routingContext.pathParam(REALM);
    routingContext.put(REALM, realm);
    routingContext.put(SESSION_ID, sessionId);
    routingContext.put(RESPONSE_TYPE, routingContext.queryParams().get(RESPONSE_TYPE));
    // optional parameter
    routingContext.put(RESPONSE_MODE, routingContext.queryParams().get(RESPONSE_MODE));
    renderHelper.renderTemplate(routingContext, "loginPage.ftl", "text/html");
  }
}
