package com.tngtech.keycloakmock.impl.handler;

import com.tngtech.keycloakmock.impl.helper.TokenHelper;
import io.jsonwebtoken.Claims;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class TokenIntrospectionRoute implements Handler<RoutingContext> {

  private static final Logger LOG = LoggerFactory.getLogger(TokenIntrospectionRoute.class);

  private static final String TOKEN_PREFIX = "token=";
  private static final String ACTIVE_CLAIM = "active";

  @Nonnull private final TokenHelper tokenHelper;

  @Inject
  public TokenIntrospectionRoute(@Nonnull TokenHelper tokenHelper) {
    this.tokenHelper = tokenHelper;
  }

  @Override
  public void handle(RoutingContext routingContext) {
    LOG.info(
        "Inside TokenIntrospectionRoute. Request body is : {}", routingContext.body().asString());

    String body = routingContext.body().asString();

    if (!body.startsWith(TOKEN_PREFIX)) {
      routingContext
          .response()
          .putHeader("content-type", "application/json")
          .setStatusCode(HttpResponseStatus.BAD_REQUEST.code())
          .end("Invalid request body");
    }

    String token = body.replaceFirst("^" + TOKEN_PREFIX, "");

    LOG.debug("Received a request to introspect token : {}", token);

    Map<String, Object> claims;
    try {
      claims = tokenHelper.parseToken(token);
    } catch (Exception e) {
      // If the token is invalid, initialize an empty claims map
      claims = new HashMap<>();
    }

    // To support various use cases, we are returning the same claims as the input token
    Map<String, Object> responseClaims = new HashMap<>(claims);

    if (responseClaims.get(Claims.EXPIRATION) != null
        && isExpiryTimeInFuture(responseClaims.get(Claims.EXPIRATION).toString())) {
      LOG.debug("Introspected token is valid");
      responseClaims.put(ACTIVE_CLAIM, true);
    } else {
      LOG.debug("Introspected token is invalid");
      responseClaims.put(ACTIVE_CLAIM, false);
    }

    routingContext
        .response()
        .putHeader("content-type", "application/json")
        .end(Json.encode(responseClaims));
  }

  private boolean isExpiryTimeInFuture(String expiryTime) {
    long currentTimeInSec = Instant.now().getEpochSecond();
    long expiryTimeInSec = Long.parseLong(expiryTime);
    return currentTimeInSec < expiryTimeInSec;
  }
}
