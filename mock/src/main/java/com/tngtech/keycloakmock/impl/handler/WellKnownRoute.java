package com.tngtech.keycloakmock.impl.handler;

import static com.tngtech.keycloakmock.impl.handler.RequestUrlConfigurationHandler.CTX_REQUEST_CONFIGURATION;

import com.tngtech.keycloakmock.impl.UrlConfiguration;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import java.util.Arrays;
import java.util.Collections;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class WellKnownRoute implements Handler<RoutingContext> {

  @Inject
  WellKnownRoute() {}

  @Override
  public void handle(@Nonnull RoutingContext routingContext) {
    UrlConfiguration requestConfiguration = routingContext.get(CTX_REQUEST_CONFIGURATION);
    routingContext
        .response()
        .putHeader("content-type", "application/json")
        .end(getConfiguration(requestConfiguration).encode());
  }

  private JsonObject getConfiguration(@Nonnull UrlConfiguration requestConfiguration) {
    JsonObject result = new JsonObject();
    result
        .put("issuer", requestConfiguration.getIssuer().toASCIIString())
        .put(
            "authorization_endpoint",
            requestConfiguration.getAuthorizationEndpoint().toASCIIString())
        .put("token_endpoint", requestConfiguration.getTokenEndpoint().toASCIIString())
        .put("jwks_uri", requestConfiguration.getJwksUri().toASCIIString())
        .put("end_session_endpoint", requestConfiguration.getEndSessionEndpoint().toASCIIString())
        .put(
            "response_types_supported",
            new JsonArray(Arrays.asList("code", "code id_token", "id_token", "token id_token")))
        .put("subject_types_supported", new JsonArray(Collections.singletonList("public")))
        .put(
            "id_token_signing_alg_values_supported",
            new JsonArray(Collections.singletonList("RS256")))
        .put("introspection_endpoint", requestConfiguration.getTokenIntrospectionEndPoint());
    return result;
  }
}
