package com.tngtech.keycloakmock.impl.handler;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.impl.MimeMapping;
import io.vertx.ext.web.RoutingContext;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Scanner;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceFileHandler implements Handler<RoutingContext> {
  private static final Logger LOG = LoggerFactory.getLogger(ResourceFileHandler.class);

  @Nonnull private final String resource;
  @Nonnull private final String contentType;

  public ResourceFileHandler(@Nonnull String resource) {
    this.resource = resource;
    this.contentType = MimeMapping.getMimeTypeForFilename(resource);
  }

  @Override
  public void handle(RoutingContext routingContext) {
    Optional<String> resourceContent = loadResourceAsString(resource);
    if (resourceContent.isPresent()) {
      routingContext
          .response()
          .putHeader(HttpHeaders.CONTENT_TYPE, contentType)
          .end(resourceContent.get());
    } else {
      LOG.error("Unable to find resource {}", resource);
      routingContext.fail(404);
    }
  }

  private Optional<String> loadResourceAsString(String resource) {
    InputStream inputStream = getClass().getResourceAsStream(resource);
    if (inputStream == null) {
      return Optional.empty();
    }
    try (Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name())) {
      return Optional.of(scanner.useDelimiter("\\A").next());
    }
  }
}
