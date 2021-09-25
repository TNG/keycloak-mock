package com.tngtech.keycloakmock.impl.handler;

import com.tngtech.keycloakmock.impl.UrlConfiguration;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RequestUrlConfigurationHandler implements Handler<RoutingContext> {
  public static final String CTX_REQUEST_CONFIGURATION = "requestConfiguration";

  private static final Pattern REALM_PATTERN = Pattern.compile("/auth/realms/([^/]*)(?:$|/.*)");

  @Nonnull private final UrlConfiguration baseConfiguration;

  @Inject
  RequestUrlConfigurationHandler(@Nonnull UrlConfiguration baseConfiguration) {
    this.baseConfiguration = Objects.requireNonNull(baseConfiguration);
  }

  @Override
  public void handle(@Nonnull RoutingContext routingContext) {
    String requestHostname = routingContext.request().getHeader("Host");
    String requestRealm = null;
    Matcher matcher = REALM_PATTERN.matcher(routingContext.normalizedPath());
    if (matcher.matches()) {
      requestRealm = matcher.group(1);
    }
    routingContext.put(
        CTX_REQUEST_CONFIGURATION,
        baseConfiguration.forRequestContext(requestHostname, requestRealm));
    routingContext.next();
  }
}
