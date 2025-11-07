package com.tngtech.keycloakmock.impl.handler;

import static io.netty.handler.codec.http.HttpHeaderNames.AUTHORIZATION;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Some endpoints, e.g. token and token introspection, optionally require client authentication.
 *
 * <p>According to RFC 6749 (OAuth 2.0 core), this can be provided either via basic authentication
 * (preferred) or via form parameters 'client_id' and 'client_secret'.
 *
 * <p>Since not all use cases require authentication, we gracefully accept missing credentials and
 * return an empty (but authenticated) user object to vertx. Even if credentials are present, no
 * actual verification is done. The data is merely extracted, so that they are available in the
 * corresponding routes via the user object.
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc6749#section-2.3.1">RFC 6749, section
 *     2.3.1</a>
 */
@Singleton
public class OptionalClientAuthHandler {
  private static final Logger LOG = LoggerFactory.getLogger(OptionalClientAuthHandler.class);

  @Inject
  OptionalClientAuthHandler() {}

  public Future<User> handle(@Nonnull RoutingContext routingContext) {
    JsonObject clientCredentials;
    String authorization = routingContext.request().getHeader(AUTHORIZATION);
    if (authorization != null) {
      if (authorization.startsWith("Basic ")) {
        clientCredentials = extractFromBasicAuth(authorization);
      } else {
        // While RFC 7662 talks about allowing Bearer tokens for client authentication,
        // I don't currently see the need to implement this.
        LOG.warn("Unexpected Authorization header ignored: {}", authorization);
        clientCredentials = new JsonObject();
      }
    } else {
      clientCredentials = extractFromFormAttributes(routingContext);
    }
    return Future.succeededFuture(User.create(clientCredentials));
  }

  private JsonObject extractFromBasicAuth(String authorization) {
    try {
      String clientIdClientSecret =
          new String(
              Base64.getDecoder().decode(authorization.replace("Basic ", "")),
              StandardCharsets.UTF_8);
      String[] split = clientIdClientSecret.split(":", 2);
      JsonObject clientCredentials = new JsonObject();
      clientCredentials.put("client_id", split[0]);
      if (split.length == 2) {
        clientCredentials.put("client_secret", split[1]);
      }
      return clientCredentials;
    } catch (RuntimeException e) {
      LOG.warn("Unable to parse authorization header {}", authorization);
      return new JsonObject();
    }
  }

  private JsonObject extractFromFormAttributes(RoutingContext routingContext) {
    JsonObject clientCredentials = new JsonObject();
    String clientId = routingContext.request().getFormAttribute("client_id");
    if (clientId != null && !clientId.isEmpty()) {
      clientCredentials.put("client_id", clientId);
    }
    String clientSecret = routingContext.request().getFormAttribute("client_secret");
    if (clientSecret != null && !clientSecret.isEmpty()) {
      clientCredentials.put("client_secret", clientSecret);
    }
    return clientCredentials;
  }
}
