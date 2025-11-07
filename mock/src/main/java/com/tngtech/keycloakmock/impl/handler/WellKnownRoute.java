package com.tngtech.keycloakmock.impl.handler;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;

import com.tngtech.keycloakmock.impl.UrlConfiguration;
import com.tngtech.keycloakmock.impl.UrlConfigurationFactory;
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

  @Nonnull private final UrlConfigurationFactory urlConfigurationFactory;

  @Inject
  WellKnownRoute(@Nonnull UrlConfigurationFactory urlConfigurationFactory) {
    this.urlConfigurationFactory = urlConfigurationFactory;
  }

  @Override
  public void handle(@Nonnull RoutingContext routingContext) {
    UrlConfiguration requestConfiguration = urlConfigurationFactory.create(routingContext);
    routingContext
        .response()
        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
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
        .put(
            "introspection_endpoint",
            requestConfiguration.getTokenIntrospectionEndpoint().toASCIIString())
        .put("jwks_uri", requestConfiguration.getJwksUri().toASCIIString())
        .put("end_session_endpoint", requestConfiguration.getEndSessionEndpoint().toASCIIString())
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
