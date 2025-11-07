package com.tngtech.keycloakmock.impl.handler;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;

import com.tngtech.keycloakmock.impl.TokenGenerator;
import io.jsonwebtoken.Claims;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class TokenIntrospectionRoute implements Handler<RoutingContext> {
  private static final Logger LOG = LoggerFactory.getLogger(TokenIntrospectionRoute.class);

  private static final String TOKEN = "token";

  private final TokenGenerator tokenGenerator;

  @Inject
  TokenIntrospectionRoute(@Nonnull TokenGenerator tokenGenerator) {
    this.tokenGenerator = tokenGenerator;
  }

  @Override
  public void handle(@Nonnull RoutingContext routingContext) {
    String clientId =
        Optional.ofNullable(routingContext.user())
            .map(u -> u.<String>get("client_id"))
            .orElse(null);
    if (clientId == null || clientId.isEmpty()) {
      routingContext.fail(401);
      return;
    }

    String token = routingContext.request().getFormAttribute(TOKEN);

    JsonObject response = new JsonObject();
    try {
      Claims claims = tokenGenerator.parseToken(token);
      if (claims.getAudience().contains(clientId)) {
        claims.forEach(response::put);
        routingContext
            .response()
            .putHeader(CONTENT_TYPE, APPLICATION_JSON)
            .end(response.put("active", true).encode());
        return;
      } else {
        LOG.warn("Requesting client {} is not a target audience of the token", clientId);
      }
    } catch (Exception e) {
      LOG.error("Failed to parse token", e);
    }
    routingContext
        .response()
        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .end(response.put("active", false).encode());
  }
}
