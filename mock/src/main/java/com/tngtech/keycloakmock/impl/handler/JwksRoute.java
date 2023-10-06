package com.tngtech.keycloakmock.impl.handler;

import io.jsonwebtoken.security.Jwks;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import java.security.PublicKey;
import java.util.Collections;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class JwksRoute implements Handler<RoutingContext> {
  private final String jwksResponse;

  @Inject
  JwksRoute(@Nonnull @Named("keyId") String keyId, @Nonnull PublicKey publicKey) {
    this.jwksResponse =
        new JsonObject()
            .put("keys", new JsonArray(Collections.singletonList(toSigningKey(keyId, publicKey))))
            .encode();
  }

  @Override
  public void handle(@Nonnull RoutingContext routingContext) {
    routingContext.response().putHeader("content-type", "application/json").end(jwksResponse);
  }

  private static JsonObject toSigningKey(@Nonnull String keyId, @Nonnull PublicKey publicKey) {
    return new JsonObject(
        Jwks.json(Jwks.builder().key(publicKey).id(keyId).publicKeyUse("sig").build()));
  }
}
