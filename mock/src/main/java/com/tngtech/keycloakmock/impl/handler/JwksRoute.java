package com.tngtech.keycloakmock.impl.handler;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Collections;
import javax.annotation.Nonnull;

public class JwksRoute implements Handler<RoutingContext> {
  private final String jwksResponse;

  public JwksRoute(
      @Nonnull final String keyId,
      @Nonnull final String algorithm,
      @Nonnull final PublicKey publicKey) {
    this.jwksResponse =
        new JsonObject()
            .put(
                "keys",
                new JsonArray(Collections.singletonList(toSigningKey(keyId, algorithm, publicKey))))
            .encode();
  }

  @Override
  public void handle(@Nonnull final RoutingContext routingContext) {
    routingContext.response().putHeader("content-type", "application/json").end(jwksResponse);
  }

  private JsonObject toSigningKey(
      @Nonnull final String keyId,
      @Nonnull final String algorithm,
      @Nonnull final PublicKey publicKey) {
    JsonObject result = new JsonObject();
    result.put("kid", keyId);
    result.put("use", "sig");
    result.put("alg", algorithm);
    if (publicKey instanceof RSAPublicKey) {
      result.put("kty", "RSA");
      RSAPublicKey rsaKey = (RSAPublicKey) publicKey;
      result.put("n", Base64.getUrlEncoder().encodeToString(rsaKey.getModulus().toByteArray()));
      result.put(
          "e", Base64.getUrlEncoder().encodeToString(rsaKey.getPublicExponent().toByteArray()));
    } else if (publicKey instanceof ECPublicKey) {
      result.put("kty", "EC");
      ECPublicKey ecKey = (ECPublicKey) publicKey;
      result.put("crv", "P-" + ecKey.getParams().getOrder().bitLength());
      result.put(
          "x", Base64.getUrlEncoder().encodeToString(ecKey.getW().getAffineX().toByteArray()));
      result.put(
          "y", Base64.getUrlEncoder().encodeToString(ecKey.getW().getAffineY().toByteArray()));
    } else {
      throw new IllegalStateException("Invalid public key type found");
    }
    return result;
  }
}
