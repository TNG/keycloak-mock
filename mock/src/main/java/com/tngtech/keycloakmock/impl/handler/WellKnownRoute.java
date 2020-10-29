package com.tngtech.keycloakmock.impl.handler;

import com.tngtech.keycloakmock.impl.UrlConfiguration;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import javax.annotation.Nonnull;

public class WellKnownRoute implements Handler<RoutingContext> {

  private final UrlConfiguration urlConfiguration;

  public WellKnownRoute(@Nonnull final UrlConfiguration urlConfiguration) {
    this.urlConfiguration = Objects.requireNonNull(urlConfiguration);
  }

  @Override
  public void handle(@Nonnull final RoutingContext routingContext) {
    final String requestRealm = routingContext.pathParam("realm");
    String requestHostname = routingContext.request().getHeader("Host");
    routingContext
        .response()
        .putHeader("content-type", "application/json")
        .end(getConfiguration(requestHostname, requestRealm).encode());
  }

  private JsonObject getConfiguration(String requestHostname, String requestRealm) {
    JsonObject result = new JsonObject();
    result
        .put("issuer", urlConfiguration.getIssuer(requestHostname, requestRealm))
        .put("authorization_endpoint", urlConfiguration.getAuthorizationEndpoint(requestHostname))
        .put("token_endpoint", urlConfiguration.getTokenEndpoint(requestHostname, requestRealm))
        .put("jwks_uri", urlConfiguration.getJwksUri(requestHostname, requestRealm))
        .put("end_session_endpoint", urlConfiguration.getEndSessionEndpoint(requestHostname))
        .put(
            "response_types_supported",
            new JsonArray(Arrays.asList("code", "code id_token", "id_token", "token id_token")))
        .put("subject_types_supported", new JsonArray(Collections.singletonList("public")))
        .put(
            "id_token_signing_alg_values_supported",
            new JsonArray(Collections.singletonList("RS256")));
    return result;
  }
}
