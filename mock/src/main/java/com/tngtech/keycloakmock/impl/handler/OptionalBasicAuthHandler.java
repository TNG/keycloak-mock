package com.tngtech.keycloakmock.impl.handler;

import io.vertx.core.Future;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.Credentials;
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials;
import io.vertx.ext.web.RoutingContext;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class OptionalBasicAuthHandler {
  private static final Logger LOG = LoggerFactory.getLogger(OptionalBasicAuthHandler.class);

  @Inject
  OptionalBasicAuthHandler() {}

  public Future<User> handle(@Nonnull RoutingContext routingContext) {
    String authorization = routingContext.request().getHeader(HttpHeaders.AUTHORIZATION);
    if (authorization != null) {
      try {
        String usernamePassword =
            new String(
                Base64.getDecoder().decode(authorization.replace("Basic ", "")),
                StandardCharsets.UTF_8);
        String[] split = usernamePassword.split(":", 2);
        Credentials credentials;
        if (split.length == 2) {
          credentials = new UsernamePasswordCredentials(split[0], split[1]);
        } else {
          credentials = new UsernamePasswordCredentials(split[0], null);
        }
        // just return the credentials if available, no need for validity check
        return Future.succeededFuture(User.create(credentials.toJson()));
      } catch (RuntimeException e) {
        LOG.warn("Unable to parse authorization header {}", authorization);
      }
    }
    // to signal that "no credentials" is OK, we still need to return an empty user
    return Future.succeededFuture(User.create(new JsonObject()));
  }
}
